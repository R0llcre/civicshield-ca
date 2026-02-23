import Foundation

struct VaultUnlocked: Equatable, Sendable {
  let params: VaultParams
  var items: [VaultItemMeta]
}

@MainActor
final class VaultStore: ObservableObject {
  @Published private(set) var unlocked: VaultUnlocked? = nil
  @Published private(set) var hasVault: Bool = false
  @Published private(set) var busy: Bool = false

  private let engine: VaultEngine

  init(engine: VaultEngine = VaultEngine()) {
    self.engine = engine
    Task { await refreshStatus() }
  }

  func refreshStatus() async {
    hasVault = await engine.hasVault()
  }

  func lock() {
    unlocked = nil
    Task {
      await engine.lock()
      await refreshStatus()
    }
  }

  func deleteVault() async throws {
    _ = try await runBusy("vault_delete") {
      try await engine.deleteVault()
    }
    unlocked = nil
    hasVault = false
  }

  func createNew(passphrase: String) async throws {
    let state = try await runBusy("vault_create") {
      try await engine.createNew(passphrase: passphrase)
    }
    unlocked = state
    hasVault = true
  }

  func unlock(passphrase: String) async throws {
    let state = try await runBusy("vault_unlock") {
      try await engine.unlock(passphrase: passphrase)
    }
    unlocked = state
    hasVault = true
  }

  func addFiles(urls: [URL]) async throws {
    let state = try await runBusy("vault_add_files") {
      try await engine.addFiles(urls: urls)
    }
    unlocked = state
    hasVault = true
  }

  func deleteItem(id: String) async throws {
    let state = try await runBusy("vault_delete_item") {
      try await engine.deleteItem(id: id)
    }
    unlocked = state
    hasVault = true
  }

  func exportZip() async throws -> Data {
    try await runBusy("vault_export_zip") {
      try await engine.exportZip()
    }
  }

  func importZip(passphrase: String, zipData: Data) async throws {
    let state = try await runBusy("vault_import_zip") {
      try await engine.importZip(passphrase: passphrase, zipData: zipData)
    }
    unlocked = state
    hasVault = true
  }

  private func runBusy<T>(_ signpost: StaticString, operation: () async throws -> T) async throws -> T {
    let interval = PerfSignpost.begin(signpost)
    busy = true
    defer {
      busy = false
      PerfSignpost.end(signpost, interval)
    }
    return try await operation()
  }
}
