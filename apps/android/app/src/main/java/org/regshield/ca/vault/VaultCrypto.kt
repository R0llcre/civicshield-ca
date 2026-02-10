package org.regshield.ca.vault

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

object VaultCrypto {
  private val UTF8 = Charsets.UTF_8
  private val rng = SecureRandom()

  fun bytesToB64(bytes: ByteArray): String = bytes.toByteString().base64()
  fun b64ToBytes(b64: String): ByteArray =
    b64.decodeBase64()?.toByteArray() ?: throw IllegalArgumentException("Invalid base64")

  fun randomSaltB64(bytes: Int = 16): String {
    val salt = ByteArray(bytes)
    rng.nextBytes(salt)
    return bytesToB64(salt)
  }

  fun deriveKey(passphrase: String, saltB64: String, iterations: Int): SecretKey {
    val salt = b64ToBytes(saltB64)
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, "AES")
  }

  fun encryptPayload(key: SecretKey, payload: ByteArray): EncryptedBlob {
    val iv = ByteArray(12)
    rng.nextBytes(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
    val ciphertext = cipher.doFinal(payload)

    return EncryptedBlob(ivB64 = bytesToB64(iv), ciphertextB64 = bytesToB64(ciphertext))
  }

  fun decryptPayload(key: SecretKey, blob: EncryptedBlob): ByteArray {
    val iv = b64ToBytes(blob.ivB64)
    val ciphertext = b64ToBytes(blob.ciphertextB64)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
    return cipher.doFinal(ciphertext)
  }

  fun encodeJson(json: String): ByteArray {
    val bytes = json.toByteArray(UTF8)
    val out = ByteArray(1 + bytes.size)
    out[0] = 1 // json
    System.arraycopy(bytes, 0, out, 1, bytes.size)
    return out
  }

  fun decodeJson(bytes: ByteArray): String {
    if (bytes.isEmpty()) throw IllegalArgumentException("Empty payload")
    val kind = bytes[0].toInt()
    if (kind != 1) throw IllegalArgumentException("Unexpected payload kind")
    return bytes.copyOfRange(1, bytes.size).toString(UTF8)
  }

  fun encodeBytes(raw: ByteArray): ByteArray {
    val out = ByteArray(1 + raw.size)
    out[0] = 2 // bytes
    System.arraycopy(raw, 0, out, 1, raw.size)
    return out
  }

  fun decodeBytes(bytes: ByteArray): ByteArray {
    if (bytes.isEmpty()) throw IllegalArgumentException("Empty payload")
    val kind = bytes[0].toInt()
    if (kind != 2) throw IllegalArgumentException("Unexpected payload kind")
    return bytes.copyOfRange(1, bytes.size)
  }
}

