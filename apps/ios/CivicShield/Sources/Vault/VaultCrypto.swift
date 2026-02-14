import CryptoKit
import Foundation

enum VaultCrypto {
  static let defaultIterations: Int = 210_000

  static func randomSaltB64(bytes: Int = 16) -> String {
    var buf = Data(count: bytes)
    _ = buf.withUnsafeMutableBytes { SecRandomCopyBytes(kSecRandomDefault, bytes, $0.baseAddress!) }
    return buf.base64EncodedString()
  }

  static func b64ToData(_ b64: String) throws -> Data {
    guard let d = Data(base64Encoded: b64) else {
      throw VaultError.invalidBase64
    }
    return d
  }

  static func dataToB64(_ data: Data) -> String {
    data.base64EncodedString()
  }

  static func deriveKey(passphrase: String, saltB64: String, iterations: Int) throws -> SymmetricKey {
    let salt = try b64ToData(saltB64)
    var derived = Data(count: 32)

    let passData = Data(passphrase.utf8)
    let result = derived.withUnsafeMutableBytes { derivedBytes in
      salt.withUnsafeBytes { saltBytes in
        passData.withUnsafeBytes { passBytes in
          CCKeyDerivationPBKDF(
            CCPBKDFAlgorithm(kCCPBKDF2),
            passBytes.bindMemory(to: Int8.self).baseAddress!,
            passData.count,
            saltBytes.bindMemory(to: UInt8.self).baseAddress!,
            salt.count,
            CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
            UInt32(iterations),
            derivedBytes.bindMemory(to: UInt8.self).baseAddress!,
            32
          )
        }
      }
    }
    guard result == kCCSuccess else { throw VaultError.kdfFailed }
    return SymmetricKey(data: derived)
  }

  static func encryptPayload(key: SymmetricKey, payload: Data) throws -> EncryptedBlob {
    var nonceData = Data(count: 12)
    _ = nonceData.withUnsafeMutableBytes { SecRandomCopyBytes(kSecRandomDefault, 12, $0.baseAddress!) }
    let nonce = try AES.GCM.Nonce(data: nonceData)
    let sealed = try AES.GCM.seal(payload, using: key, nonce: nonce)

    // Match WebCrypto/Android: ciphertext bytes include the 16-byte tag appended.
    let combinedCiphertext = sealed.ciphertext + sealed.tag

    return EncryptedBlob(
      ivB64: dataToB64(nonceData),
      ciphertextB64: dataToB64(combinedCiphertext)
    )
  }

  static func decryptPayload(key: SymmetricKey, blob: EncryptedBlob) throws -> Data {
    let nonceData = try b64ToData(blob.ivB64)
    let all = try b64ToData(blob.ciphertextB64)
    guard all.count >= 16 else { throw VaultError.decryptFailed }

    let ciphertext = all.prefix(all.count - 16)
    let tag = all.suffix(16)
    let nonce = try AES.GCM.Nonce(data: nonceData)
    let box = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
    return try AES.GCM.open(box, using: key)
  }

  // Payload encoding: 0x01 + JSON bytes (UTF-8)
  static func encodeJson(_ json: String) -> Data {
    let bytes = Data(json.utf8)
    var out = Data(capacity: 1 + bytes.count)
    out.append(0x01)
    out.append(bytes)
    return out
  }

  static func decodeJson(_ data: Data) throws -> String {
    guard let first = data.first else { throw VaultError.invalidPayload }
    guard first == 0x01 else { throw VaultError.invalidPayload }
    let rest = data.dropFirst()
    guard let s = String(data: rest, encoding: .utf8) else { throw VaultError.invalidPayload }
    return s
  }

  // Optional bytes kind (0x02) for internal storage.
  static func encodeBytes(_ bytes: Data) -> Data {
    var out = Data(capacity: 1 + bytes.count)
    out.append(0x02)
    out.append(bytes)
    return out
  }

  static func decodeBytes(_ data: Data) throws -> Data {
    guard let first = data.first else { throw VaultError.invalidPayload }
    guard first == 0x02 else { throw VaultError.invalidPayload }
    return Data(data.dropFirst())
  }
}

enum VaultError: Error, LocalizedError {
  case invalidBase64
  case kdfFailed
  case invalidZip
  case unsupportedVaultVersion
  case unsupportedAlgorithm
  case invalidPayload
  case decryptFailed
  case noVault
  case ioFailed

  var errorDescription: String? {
    switch self {
    case .invalidBase64:
      return "Invalid base64."
    case .kdfFailed:
      return "Key derivation failed."
    case .invalidZip:
      return "Invalid vault zip."
    case .unsupportedVaultVersion:
      return "Unsupported vault version."
    case .unsupportedAlgorithm:
      return "Unsupported algorithm."
    case .invalidPayload:
      return "Invalid payload."
    case .decryptFailed:
      return "Unable to unlock (wrong passphrase or corrupted vault)."
    case .noVault:
      return "No vault found."
    case .ioFailed:
      return "I/O failed."
    }
  }
}

