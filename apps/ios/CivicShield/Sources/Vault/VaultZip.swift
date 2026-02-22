import Foundation
import CryptoKit
import ZIPFoundation

enum VaultZip {
  private static let jsonEncoder: JSONEncoder = {
    let e = JSONEncoder()
    e.outputFormatting = [.sortedKeys]
    return e
  }()

  private static let jsonDecoder = JSONDecoder()

  static func exportZip(key: SymmetricKey, params: VaultParams, plain: VaultPlain) throws -> Data {
    let payloadJsonData = try jsonEncoder.encode(plain)
    guard let payloadJson = String(data: payloadJsonData, encoding: .utf8) else {
      throw VaultError.invalidPayload
    }

    let encrypted = try VaultCrypto.encryptPayload(key: key, payload: VaultCrypto.encodeJson(payloadJson))

    let manifest = VaultExportV1(
      version: 1,
      kdf: VaultKdf(saltB64: params.saltB64, iterations: params.iterations),
      alg: "AES-GCM"
    )

    let manifestData = try jsonEncoder.encode(manifest)
    let encryptedData = try jsonEncoder.encode(encrypted)

    let tmp = FileManager.default.temporaryDirectory
      .appendingPathComponent("civicshield_vault_" + UUID().uuidString)
      .appendingPathExtension("zip")
    defer { try? FileManager.default.removeItem(at: tmp) }

    guard let archive = Archive(url: tmp, accessMode: .create) else {
      throw VaultError.ioFailed
    }

    try addEntry(archive: archive, path: "vault_v1.json", data: manifestData)
    try addEntry(archive: archive, path: "vault_payload.enc", data: encryptedData)

    return try Data(contentsOf: tmp)
  }

  static func importZip(_ zipData: Data) throws -> (params: VaultParams, payload: EncryptedBlob) {
    let tmp = FileManager.default.temporaryDirectory
      .appendingPathComponent("civicshield_vault_import_" + UUID().uuidString)
      .appendingPathExtension("zip")
    defer { try? FileManager.default.removeItem(at: tmp) }

    try zipData.write(to: tmp, options: [.atomic])
    guard let archive = Archive(url: tmp, accessMode: .read) else {
      throw VaultError.invalidZip
    }

    guard let manifestEntry = archive["vault_v1.json"], let payloadEntry = archive["vault_payload.enc"] else {
      throw VaultError.invalidZip
    }

    let manifestData = try extractEntry(archive: archive, entry: manifestEntry)
    let payloadData = try extractEntry(archive: archive, entry: payloadEntry)

    let manifest = try jsonDecoder.decode(VaultExportV1.self, from: manifestData)
    guard manifest.version == 1 else { throw VaultError.unsupportedVaultVersion }
    guard manifest.alg == "AES-GCM" else { throw VaultError.unsupportedAlgorithm }

    let payload = try jsonDecoder.decode(EncryptedBlob.self, from: payloadData)
    guard !payload.ivB64.isEmpty, !payload.ciphertextB64.isEmpty else { throw VaultError.invalidPayload }

    let params = VaultParams(version: 1, saltB64: manifest.kdf.saltB64, iterations: manifest.kdf.iterations)
    return (params, payload)
  }

  private static func addEntry(archive: Archive, path: String, data: Data) throws {
    try archive.addEntry(
      with: path,
      type: .file,
      uncompressedSize: UInt32(data.count),
      compressionMethod: .deflate,
      bufferSize: 16_384,
      provider: { position, size in
        let start = Int(position)
        let end = min(start + size, data.count)
        return data.subdata(in: start..<end)
      }
    )
  }

  private static func extractEntry(archive: Archive, entry: Entry) throws -> Data {
    var out = Data()
    _ = try archive.extract(entry, bufferSize: 16_384) { chunk in
      out.append(chunk)
    }
    return out
  }
}
