package org.regshield.ca.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.SecretKey
import javax.inject.Inject
import kotlinx.serialization.json.Json
import org.regshield.ca.vault.db.VaultDao
import org.regshield.ca.vault.db.VaultDatabase
import org.regshield.ca.vault.db.VaultItemEntity
import org.regshield.ca.vault.db.VaultMetaEntity

class VaultRepository @Inject constructor(
  @ApplicationContext private val ctx: Context,
  private val db: VaultDatabase,
  private val dao: VaultDao,
  private val json: Json,
) {
  private val createdAtFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
  }

  suspend fun hasVault(): Boolean = dao.getMeta() != null

  suspend fun createNewVault(passphrase: String): VaultUnlocked {
    val params = VaultParams(
      version = 1,
      saltB64 = VaultCrypto.randomSaltB64(16),
      iterations = 210_000,
    )
    val key = VaultCrypto.deriveKey(passphrase, params.saltB64, params.iterations)
    val manifest = VaultManifest(items = emptyList())
    val manifestEnc = VaultCrypto.encryptPayload(key, VaultCrypto.encodeJson(json.encodeToString(VaultManifest.serializer(), manifest)))

    db.withTransaction {
      dao.clearItems()
      dao.upsertMeta(
        VaultMetaEntity(
          id = 1,
          version = 1,
          saltB64 = params.saltB64,
          iterations = params.iterations,
          manifestIv = VaultCrypto.b64ToBytes(manifestEnc.ivB64),
          manifestCiphertext = VaultCrypto.b64ToBytes(manifestEnc.ciphertextB64),
        ),
      )
    }

    return VaultUnlocked(params = params, key = key, items = emptyList())
  }

  suspend fun unlock(passphrase: String): VaultUnlocked {
    val meta = dao.getMeta() ?: throw IllegalStateException("No vault found. Create or import one first.")
    val params = VaultParams(version = meta.version, saltB64 = meta.saltB64, iterations = meta.iterations)
    val key = VaultCrypto.deriveKey(passphrase, params.saltB64, params.iterations)

    val items = if (meta.manifestIv != null && meta.manifestCiphertext != null) {
      val blob = EncryptedBlob(
        ivB64 = VaultCrypto.bytesToB64(meta.manifestIv),
        ciphertextB64 = VaultCrypto.bytesToB64(meta.manifestCiphertext),
      )
      val manifestJson = VaultCrypto.decodeJson(VaultCrypto.decryptPayload(key, blob))
      val manifest = json.decodeFromString(VaultManifest.serializer(), manifestJson)
      manifest.items
    } else {
      emptyList()
    }

    return VaultUnlocked(params = params, key = key, items = items)
  }

  suspend fun addFiles(unlocked: VaultUnlocked, uris: List<Uri>): VaultUnlocked {
    val current = unlocked.items.toMutableList()
    val key = unlocked.key
    val cr = ctx.contentResolver

    for (uri in uris) {
      val (name, size, type) = queryFileMeta(uri)
      val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: throw IllegalArgumentException("Failed to read file")
      val id = UUID.randomUUID().toString()
      val meta = VaultItemMeta(
        id = id,
        createdAt = createdAtFormat.format(Date()),
        originalName = name,
        contentType = type,
        size = size.takeIf { it > 0 } ?: bytes.size.toLong(),
      )

      val encrypted = VaultCrypto.encryptPayload(key, VaultCrypto.encodeBytes(bytes))
      dao.upsertItem(
        VaultItemEntity(
          id = id,
          iv = VaultCrypto.b64ToBytes(encrypted.ivB64),
          ciphertext = VaultCrypto.b64ToBytes(encrypted.ciphertextB64),
        ),
      )
      current.add(meta)
    }

    persistManifest(unlocked.params, key, current)
    return unlocked.copy(items = current)
  }

  suspend fun deleteItem(unlocked: VaultUnlocked, id: String): VaultUnlocked {
    dao.deleteItem(id)
    val next = unlocked.items.filterNot { it.id == id }
    persistManifest(unlocked.params, unlocked.key, next)
    return unlocked.copy(items = next)
  }

  suspend fun exportZip(unlocked: VaultUnlocked): ByteArray {
    val key = unlocked.key
    val files = mutableListOf<VaultFile>()
    for (m in unlocked.items) {
      val row = dao.getItem(m.id) ?: continue
      val blob = EncryptedBlob(
        ivB64 = VaultCrypto.bytesToB64(row.iv),
        ciphertextB64 = VaultCrypto.bytesToB64(row.ciphertext),
      )
      val decrypted = VaultCrypto.decryptPayload(key, blob)
      val raw = VaultCrypto.decodeBytes(decrypted)
      files.add(VaultFile(id = m.id, bytesB64 = VaultCrypto.bytesToB64(raw)))
    }
    val plain = VaultPlain(items = unlocked.items, files = files)
    return VaultZip.exportZip(json, key, unlocked.params, plain)
  }

  suspend fun importZip(passphrase: String, zipBytes: ByteArray): VaultUnlocked {
    val imported = VaultZip.importZip(json, zipBytes)
    val params = imported.params
    val key = VaultCrypto.deriveKey(passphrase, params.saltB64, params.iterations)

    // Decrypt first; only mutate DB after successful decrypt.
    val payloadBytes = VaultCrypto.decryptPayload(key, imported.payload)
    val payloadJson = VaultCrypto.decodeJson(payloadBytes)
    val plain = json.decodeFromString(VaultPlain.serializer(), payloadJson)

    val manifest = VaultManifest(items = plain.items)
    val manifestEnc = VaultCrypto.encryptPayload(key, VaultCrypto.encodeJson(json.encodeToString(VaultManifest.serializer(), manifest)))

    db.withTransaction {
      dao.clearItems()
      dao.upsertMeta(
        VaultMetaEntity(
          id = 1,
          version = 1,
          saltB64 = params.saltB64,
          iterations = params.iterations,
          manifestIv = VaultCrypto.b64ToBytes(manifestEnc.ivB64),
          manifestCiphertext = VaultCrypto.b64ToBytes(manifestEnc.ciphertextB64),
        ),
      )
      for (f in plain.files) {
        val raw = VaultCrypto.b64ToBytes(f.bytesB64)
        val enc = VaultCrypto.encryptPayload(key, VaultCrypto.encodeBytes(raw))
        dao.upsertItem(
          VaultItemEntity(
            id = f.id,
            iv = VaultCrypto.b64ToBytes(enc.ivB64),
            ciphertext = VaultCrypto.b64ToBytes(enc.ciphertextB64),
          ),
        )
      }
    }

    return VaultUnlocked(params = params, key = key, items = plain.items)
  }

  private suspend fun persistManifest(params: VaultParams, key: SecretKey, items: List<VaultItemMeta>) {
    val meta = dao.getMeta() ?: throw IllegalStateException("Vault missing")
    val manifest = VaultManifest(items = items)
    val manifestEnc = VaultCrypto.encryptPayload(key, VaultCrypto.encodeJson(json.encodeToString(VaultManifest.serializer(), manifest)))
    dao.upsertMeta(
      meta.copy(
        version = 1,
        saltB64 = params.saltB64,
        iterations = params.iterations,
        manifestIv = VaultCrypto.b64ToBytes(manifestEnc.ivB64),
        manifestCiphertext = VaultCrypto.b64ToBytes(manifestEnc.ciphertextB64),
      ),
    )
  }

  private fun queryFileMeta(uri: Uri): Triple<String, Long, String> {
    val cr = ctx.contentResolver
    var name = "file"
    var size = 0L
    cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
      if (c.moveToFirst()) {
        val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
        if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
        if (sizeIdx >= 0) size = c.getLong(sizeIdx)
      }
    }
    val type = cr.getType(uri) ?: "application/octet-stream"
    return Triple(name, size, type)
  }
}

data class VaultUnlocked(
  val params: VaultParams,
  val key: SecretKey,
  val items: List<VaultItemMeta>,
)

