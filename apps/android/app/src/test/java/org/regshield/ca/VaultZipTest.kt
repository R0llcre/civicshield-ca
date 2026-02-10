package org.regshield.ca

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Test
import org.regshield.ca.vault.VaultCrypto
import org.regshield.ca.vault.VaultFile
import org.regshield.ca.vault.VaultItemMeta
import org.regshield.ca.vault.VaultParams
import org.regshield.ca.vault.VaultPlain
import org.regshield.ca.vault.VaultZip

@OptIn(ExperimentalSerializationApi::class)
class VaultZipTest {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Test
  fun exportImport_roundtrip() {
    val params = VaultParams(version = 1, saltB64 = "AAAAAAAAAAAAAAAAAAAAAA==", iterations = 210_000)
    val key = VaultCrypto.deriveKey("pw", params.saltB64, params.iterations)

    val items = listOf(
      VaultItemMeta(
        id = "item-1",
        createdAt = "2026-02-10T00:00:00.000Z",
        originalName = "a.txt",
        contentType = "text/plain",
        size = 3,
      ),
      VaultItemMeta(
        id = "item-2",
        createdAt = "2026-02-10T00:00:00.000Z",
        originalName = "b.bin",
        contentType = "application/octet-stream",
        size = 2,
      ),
    )

    val plain = VaultPlain(
      items = items,
      files = listOf(
        VaultFile(id = "item-1", bytesB64 = VaultCrypto.bytesToB64(byteArrayOf(1, 2, 3))),
        VaultFile(id = "item-2", bytesB64 = VaultCrypto.bytesToB64(byteArrayOf(9, 8))),
      ),
    )

    val zip = VaultZip.exportZip(json, key, params, plain)
    val imported = VaultZip.importZip(json, zip)
    assertThat(imported.params).isEqualTo(params)

    val decrypted = VaultCrypto.decryptPayload(key, imported.payload)
    val payloadJson = VaultCrypto.decodeJson(decrypted)
    val out = json.decodeFromString(VaultPlain.serializer(), payloadJson)
    assertThat(out).isEqualTo(plain)
  }
}
