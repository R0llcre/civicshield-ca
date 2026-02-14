import XCTest

@testable import CivicShield

final class VaultZipTests: XCTestCase {
  func testImportWebFixture() throws {
    let bundle = Bundle(for: Self.self)
    guard let url = bundle.url(forResource: "vault_web_fixture", withExtension: "zip") else {
      XCTFail("Missing vault_web_fixture.zip in test bundle")
      return
    }

    let zipData = try Data(contentsOf: url)
    let imported = try VaultZip.importZip(zipData)

    // Fixture passphrase is defined by scripts/generate_vault_fixtures.mjs
    let key = try VaultCrypto.deriveKey(
      passphrase: "web-fixture-passphrase",
      saltB64: imported.params.saltB64,
      iterations: imported.params.iterations
    )

    let payloadBytes = try VaultCrypto.decryptPayload(key: key, blob: imported.payload)
    let payloadJson = try VaultCrypto.decodeJson(payloadBytes)
    guard let payloadData = payloadJson.data(using: .utf8) else {
      XCTFail("Fixture payload JSON is not UTF-8")
      return
    }

    let plain = try JSONDecoder().decode(VaultPlain.self, from: payloadData)
    XCTAssertEqual(plain.items.count, 2)
    XCTAssertEqual(plain.files.count, 2)

    XCTAssertEqual(plain.items[0].id, "web-item-1")
    XCTAssertEqual(plain.items[1].id, "web-item-2")

    let fileById = Dictionary(uniqueKeysWithValues: plain.files.map { ($0.id, $0.bytesB64) })
    XCTAssertEqual(Data(base64Encoded: fileById["web-item-1"] ?? "") ?? Data(), Data("hello".utf8))
    XCTAssertEqual(Data(base64Encoded: fileById["web-item-2"] ?? "") ?? Data(), Data([1, 2, 3]))
  }
}

