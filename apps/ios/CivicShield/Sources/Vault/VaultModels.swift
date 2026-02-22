import Foundation

struct VaultParams: Codable, Equatable {
  let version: Int
  let saltB64: String
  let iterations: Int

  enum CodingKeys: String, CodingKey {
    case version
    case saltB64
    case iterations
  }
}

struct VaultItemMeta: Codable, Identifiable, Equatable {
  let id: String
  let createdAt: String
  let originalName: String
  let contentType: String
  let size: Int64
}

struct VaultFile: Codable, Equatable {
  let id: String
  let bytesB64: String

  enum CodingKeys: String, CodingKey {
    case id
    case bytesB64
  }
}

struct VaultPlain: Codable, Equatable {
  let items: [VaultItemMeta]
  let files: [VaultFile]
}

struct EncryptedBlob: Codable, Equatable {
  let ivB64: String
  let ciphertextB64: String

  enum CodingKeys: String, CodingKey {
    case ivB64
    case ciphertextB64
  }
}

// Zip manifest schema (shared with Android/Web)
struct VaultExportV1: Codable, Equatable {
  let version: Int
  let kdf: VaultKdf
  let alg: String
}

struct VaultKdf: Codable, Equatable {
  let saltB64: String
  let iterations: Int
}

