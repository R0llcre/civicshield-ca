package org.regshield.ca

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Test
import org.regshield.ca.vault.VaultCrypto
import org.regshield.ca.vault.VaultPlain
import org.regshield.ca.vault.VaultZip

@OptIn(ExperimentalSerializationApi::class)
class VaultWebFixtureImportTest {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Test
  fun import_webFixture_zip_decryptsAndParses() {
    val bytes = javaClass.classLoader
      ?.getResourceAsStream("vault_web_fixture.zip")
      ?.use { it.readBytes() }
      ?: throw IllegalStateException("Missing test resource vault_web_fixture.zip")

    val imported = VaultZip.importZip(json, bytes)
    val key = VaultCrypto.deriveKey("web-fixture-passphrase", imported.params.saltB64, imported.params.iterations)

    val payloadBytes = VaultCrypto.decryptPayload(key, imported.payload)
    val payloadJson = VaultCrypto.decodeJson(payloadBytes)
    val plain = json.decodeFromString(VaultPlain.serializer(), payloadJson)

    assertThat(plain.items).hasSize(2)
    assertThat(plain.items[0].originalName).isEqualTo("web-hello.txt")
    assertThat(plain.items[1].originalName).isEqualTo("web-evidence.bin")

    val file1 = plain.files.first { it.id == "web-item-1" }
    val file2 = plain.files.first { it.id == "web-item-2" }

    assertThat(String(VaultCrypto.b64ToBytes(file1.bytesB64), Charsets.UTF_8)).isEqualTo("hello")
    assertThat(VaultCrypto.b64ToBytes(file2.bytesB64)).isEqualTo(byteArrayOf(1, 2, 3))
  }
}
