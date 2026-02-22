import XCTest

@testable import CivicShield

final class VaultCryptoTests: XCTestCase {
  func testEncryptDecryptRoundtrip() throws {
    let salt = Data(repeating: 7, count: 16).base64EncodedString()
    let key = try VaultCrypto.deriveKey(passphrase: "pw", saltB64: salt, iterations: VaultCrypto.defaultIterations)

    let payload = VaultCrypto.encodeJson("{\"hello\":\"world\"}")
    let enc = try VaultCrypto.encryptPayload(key: key, payload: payload)
    let dec = try VaultCrypto.decryptPayload(key: key, blob: enc)

    let json = try VaultCrypto.decodeJson(dec)
    XCTAssertEqual(json, "{\"hello\":\"world\"}")
  }

  func testWrongPassphraseFailsToDecrypt() throws {
    let salt = Data(repeating: 9, count: 16).base64EncodedString()
    let k1 = try VaultCrypto.deriveKey(passphrase: "pw1", saltB64: salt, iterations: VaultCrypto.defaultIterations)
    let k2 = try VaultCrypto.deriveKey(passphrase: "pw2", saltB64: salt, iterations: VaultCrypto.defaultIterations)

    let payload = VaultCrypto.encodeBytes(Data([1, 2, 3, 4]))
    let enc = try VaultCrypto.encryptPayload(key: k1, payload: payload)

    XCTAssertThrowsError(try VaultCrypto.decryptPayload(key: k2, blob: enc))
  }
}

