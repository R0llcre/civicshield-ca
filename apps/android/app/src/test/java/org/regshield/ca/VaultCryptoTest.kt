package org.regshield.ca

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.regshield.ca.vault.EncryptedBlob
import org.regshield.ca.vault.VaultCrypto

class VaultCryptoTest {
  @Test
  fun encryptDecrypt_jsonRoundtrip() {
    val saltB64 = "AAAAAAAAAAAAAAAAAAAAAA=="
    val key = VaultCrypto.deriveKey("passphrase", saltB64, 210_000)

    val json = """{"hello":"world"}"""
    val blob: EncryptedBlob = VaultCrypto.encryptPayload(key, VaultCrypto.encodeJson(json))

    val decrypted = VaultCrypto.decryptPayload(key, blob)
    val outJson = VaultCrypto.decodeJson(decrypted)
    assertThat(outJson).isEqualTo(json)
  }

  @Test
  fun decrypt_wrongPassphrase_fails() {
    val saltB64 = "AAAAAAAAAAAAAAAAAAAAAA=="
    val keyA = VaultCrypto.deriveKey("passphrase-a", saltB64, 210_000)
    val keyB = VaultCrypto.deriveKey("passphrase-b", saltB64, 210_000)

    val blob = VaultCrypto.encryptPayload(keyA, VaultCrypto.encodeJson("""{"x":1}"""))
    val err = runCatching { VaultCrypto.decryptPayload(keyB, blob) }.exceptionOrNull()
    assertThat(err).isNotNull()
  }
}

