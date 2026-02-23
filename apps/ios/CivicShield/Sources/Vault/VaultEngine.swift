import CryptoKit
import Foundation
import UniformTypeIdentifiers

private struct VaultManifest: Codable, Equatable {
  let items: [VaultItemMeta]
}

private struct VaultLocalMeta: Codable, Equatable {
  let params: VaultParams
  let manifest: EncryptedBlob
}

private struct VaultUnlockedInternal {
  let params: VaultParams
  let key: SymmetricKey
  var items: [VaultItemMeta]
}

actor VaultEngine {
  private let fm = FileManager.default
  private let encoder: JSONEncoder = {
    let e = JSONEncoder()
    e.outputFormatting = [.sortedKeys]
    return e
  }()
  private let decoder = JSONDecoder()

  private var unlocked: VaultUnlockedInternal? = nil

  private var vaultDir: URL {
    let base = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    return base.appendingPathComponent("CivicShield", isDirectory: true)
      .appendingPathComponent("Vault", isDirectory: true)
  }

  private var metaURL: URL { vaultDir.appendingPathComponent("meta.json") }
  private var itemsDir: URL { vaultDir.appendingPathComponent("items", isDirectory: true) }

  func hasVault() -> Bool {
    fm.fileExists(atPath: metaURL.path)
  }

  func lock() {
    unlocked = nil
  }

  func deleteVault() throws {
    unlocked = nil
    if fm.fileExists(atPath: vaultDir.path) {
      try fm.removeItem(at: vaultDir)
    }
  }

  func createNew(passphrase: String) throws -> VaultUnlocked {
    try ensureDirs()
    try clearItems()

    let params = VaultParams(version: 1, saltB64: VaultCrypto.randomSaltB64(bytes: 16), iterations: VaultCrypto.defaultIterations)
    let key = try VaultCrypto.deriveKey(passphrase: passphrase, saltB64: params.saltB64, iterations: params.iterations)

    let manifest = VaultManifest(items: [])
    let manifestBlob = try encryptManifest(key: key, manifest: manifest)
    let meta = VaultLocalMeta(params: params, manifest: manifestBlob)
    try writeMeta(meta)

    let state = VaultUnlockedInternal(params: params, key: key, items: [])
    unlocked = state
    return VaultUnlocked(params: state.params, items: state.items)
  }

  func unlock(passphrase: String) throws -> VaultUnlocked {
    let meta = try readMeta()
    let key = try VaultCrypto.deriveKey(passphrase: passphrase, saltB64: meta.params.saltB64, iterations: meta.params.iterations)

    let items = try decryptManifest(key: key, blob: meta.manifest).items
    let state = VaultUnlockedInternal(params: meta.params, key: key, items: items)
    unlocked = state
    return VaultUnlocked(params: state.params, items: state.items)
  }

  func addFiles(urls: [URL]) throws -> VaultUnlocked {
    guard var u = unlocked else { throw VaultError.noVault }

    for url in urls {
      let (name, type) = fileMeta(url: url)
      let bytes = try Data(contentsOf: url)
      let id = UUID().uuidString.lowercased()
      let createdAt = isoNow()
      let meta = VaultItemMeta(
        id: id,
        createdAt: createdAt,
        originalName: name,
        contentType: type,
        size: Int64(bytes.count)
      )

      let enc = try VaultCrypto.encryptPayload(key: u.key, payload: VaultCrypto.encodeBytes(bytes))
      let encData = try encoder.encode(enc)
      try encData.write(to: itemsDir.appendingPathComponent("\(id).blob"), options: [.atomic])

      u.items.append(meta)
    }

    try persistManifest(params: u.params, key: u.key, items: u.items)
    unlocked = u
    return VaultUnlocked(params: u.params, items: u.items)
  }

  func deleteItem(id: String) throws -> VaultUnlocked {
    guard var u = unlocked else { throw VaultError.noVault }
    let path = itemsDir.appendingPathComponent("\(id).blob")
    try? fm.removeItem(at: path)
    u.items.removeAll { $0.id == id }
    try persistManifest(params: u.params, key: u.key, items: u.items)
    unlocked = u
    return VaultUnlocked(params: u.params, items: u.items)
  }

  func exportZip() throws -> Data {
    guard let u = unlocked else { throw VaultError.noVault }

    var files: [VaultFile] = []
    files.reserveCapacity(u.items.count)

    for item in u.items {
      let path = itemsDir.appendingPathComponent("\(item.id).blob")
      let encData = try Data(contentsOf: path)
      let blob = try decoder.decode(EncryptedBlob.self, from: encData)
      let payload = try VaultCrypto.decryptPayload(key: u.key, blob: blob)
      let raw = try VaultCrypto.decodeBytes(payload)
      files.append(VaultFile(id: item.id, bytesB64: raw.base64EncodedString()))
    }

    let plain = VaultPlain(items: u.items, files: files)
    return try VaultZip.exportZip(key: u.key, params: u.params, plain: plain)
  }

  func importZip(passphrase: String, zipData: Data) throws -> VaultUnlocked {
    // Parse + decrypt first (no mutation).
    let imported = try VaultZip.importZip(zipData)
    let key = try VaultCrypto.deriveKey(passphrase: passphrase, saltB64: imported.params.saltB64, iterations: imported.params.iterations)
    let payloadBytes = try VaultCrypto.decryptPayload(key: key, blob: imported.payload)
    let payloadJson = try VaultCrypto.decodeJson(payloadBytes)
    guard let payloadData = payloadJson.data(using: .utf8) else { throw VaultError.invalidPayload }
    let plain = try decoder.decode(VaultPlain.self, from: payloadData)

    try ensureDirs()
    try clearItems()

    for file in plain.files {
      guard let raw = Data(base64Encoded: file.bytesB64) else { throw VaultError.invalidBase64 }
      let enc = try VaultCrypto.encryptPayload(key: key, payload: VaultCrypto.encodeBytes(raw))
      let encData = try encoder.encode(enc)
      try encData.write(to: itemsDir.appendingPathComponent("\(file.id).blob"), options: [.atomic])
    }

    let manifest = VaultManifest(items: plain.items)
    let manifestBlob = try encryptManifest(key: key, manifest: manifest)
    let meta = VaultLocalMeta(params: imported.params, manifest: manifestBlob)
    try writeMeta(meta)

    let state = VaultUnlockedInternal(params: imported.params, key: key, items: plain.items)
    unlocked = state
    return VaultUnlocked(params: state.params, items: state.items)
  }

  private func ensureDirs() throws {
    try fm.createDirectory(at: itemsDir, withIntermediateDirectories: true)
  }

  private func clearItems() throws {
    if fm.fileExists(atPath: itemsDir.path) {
      let files = (try? fm.contentsOfDirectory(at: itemsDir, includingPropertiesForKeys: nil)) ?? []
      for file in files {
        try? fm.removeItem(at: file)
      }
    }
  }

  private func readMeta() throws -> VaultLocalMeta {
    guard fm.fileExists(atPath: metaURL.path) else { throw VaultError.noVault }
    let data = try Data(contentsOf: metaURL)
    return try decoder.decode(VaultLocalMeta.self, from: data)
  }

  private func writeMeta(_ meta: VaultLocalMeta) throws {
    let data = try encoder.encode(meta)
    try data.write(to: metaURL, options: [.atomic])
  }

  private func encryptManifest(key: SymmetricKey, manifest: VaultManifest) throws -> EncryptedBlob {
    let data = try encoder.encode(manifest)
    guard let json = String(data: data, encoding: .utf8) else { throw VaultError.invalidPayload }
    return try VaultCrypto.encryptPayload(key: key, payload: VaultCrypto.encodeJson(json))
  }

  private func decryptManifest(key: SymmetricKey, blob: EncryptedBlob) throws -> VaultManifest {
    let data = try VaultCrypto.decryptPayload(key: key, blob: blob)
    let json = try VaultCrypto.decodeJson(data)
    guard let bytes = json.data(using: .utf8) else { throw VaultError.invalidPayload }
    return try decoder.decode(VaultManifest.self, from: bytes)
  }

  private func persistManifest(params: VaultParams, key: SymmetricKey, items: [VaultItemMeta]) throws {
    let blob = try encryptManifest(key: key, manifest: VaultManifest(items: items))
    let meta = VaultLocalMeta(params: params, manifest: blob)
    try writeMeta(meta)
  }

  private func fileMeta(url: URL) -> (name: String, mime: String) {
    let name = url.lastPathComponent.isEmpty ? "file" : url.lastPathComponent
    let ext = url.pathExtension
    let type = UTType(filenameExtension: ext)?.preferredMIMEType ?? "application/octet-stream"
    return (name, type)
  }

  private func isoNow() -> String {
    ISO8601DateFormatter().string(from: Date())
  }
}
