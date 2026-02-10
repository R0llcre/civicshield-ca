package org.regshield.ca.vault

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class VaultExportV1(
  val version: Int = 1,
  val kdf: VaultKdf,
  val alg: String = "AES-GCM",
)

@Serializable
private data class VaultKdf(
  @SerialName("saltB64") val saltB64: String,
  val iterations: Int,
)

object VaultZip {
  fun exportZip(json: Json, key: javax.crypto.SecretKey, params: VaultParams, plain: VaultPlain): ByteArray {
    // Encrypt a single payload to avoid leaking counts/filenames on wrong passphrase.
    val payloadJson = json.encodeToString(VaultPlain.serializer(), plain)
    val encrypted = VaultCrypto.encryptPayload(key, VaultCrypto.encodeJson(payloadJson))

    val manifest = VaultExportV1(
      version = 1,
      kdf = VaultKdf(saltB64 = params.saltB64, iterations = params.iterations),
      alg = "AES-GCM",
    )

    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zos ->
      zos.putNextEntry(ZipEntry("vault_v1.json"))
      zos.write(json.encodeToString(VaultExportV1.serializer(), manifest).toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("vault_payload.enc"))
      zos.write(json.encodeToString(EncryptedBlob.serializer(), encrypted).toByteArray(Charsets.UTF_8))
      zos.closeEntry()
    }
    return out.toByteArray()
  }

  data class ImportedZip(
    val params: VaultParams,
    val payload: EncryptedBlob,
  )

  fun importZip(json: Json, zipBytes: ByteArray): ImportedZip {
    val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
    var manifestJson: String? = null
    var payloadJson: String? = null
    zis.use { z ->
      while (true) {
        val entry = z.nextEntry ?: break
        val bytes = z.readBytes()
        when (entry.name) {
          "vault_v1.json" -> manifestJson = bytes.toString(Charsets.UTF_8)
          "vault_payload.enc" -> payloadJson = bytes.toString(Charsets.UTF_8)
        }
        z.closeEntry()
      }
    }

    if (manifestJson.isNullOrBlank() || payloadJson.isNullOrBlank()) {
      throw IllegalArgumentException("Invalid vault zip")
    }

    val manifest = json.decodeFromString(VaultExportV1.serializer(), manifestJson!!)
    if (manifest.version != 1) throw IllegalArgumentException("Unsupported vault version")
    if (manifest.alg != "AES-GCM") throw IllegalArgumentException("Unsupported algorithm")

    val payload = json.decodeFromString(EncryptedBlob.serializer(), payloadJson!!)
    if (payload.ivB64.isBlank() || payload.ciphertextB64.isBlank()) throw IllegalArgumentException("Invalid payload")

    return ImportedZip(
      params = VaultParams(version = 1, saltB64 = manifest.kdf.saltB64, iterations = manifest.kdf.iterations),
      payload = payload,
    )
  }
}

