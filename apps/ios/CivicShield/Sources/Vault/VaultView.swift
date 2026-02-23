import SwiftUI
import UniformTypeIdentifiers

struct VaultView: View {
  @StateObject private var store = VaultStore()

  @State private var passphrase: String = ""
  @State private var confirmPassphrase: String = ""
  @State private var error: String? = nil

  @State private var showAddFiles = false
  @State private var showImportZip = false
  @State private var exportDoc = DataDocument(data: Data())
  @State private var showExporter = false
  @State private var showDeleteVaultConfirm = false

  var body: some View {
    ScrollView {
      LazyVStack(alignment: .leading, spacing: 14) {
        GlassCard {
          Text("Evidence Vault")
            .font(.system(.title, design: .rounded).weight(.bold))
            .foregroundStyle(Color(red: 0.06, green: 0.11, blue: 0.23))
          Text("Local-only encrypted storage for files. Nothing uploads by default.")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)
        }

        if let unlocked = store.unlocked {
          unlockedSection(unlocked: unlocked)
        } else {
          lockedSection(hasVault: store.hasVault)
        }

        if let error {
          GlassCard {
            Text(error)
              .foregroundStyle(.red)
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
          }
        }

        GlassCard {
          Text("Information only. Not legal advice.")
            .font(.system(.caption, design: .rounded))
            .foregroundStyle(.secondary)
        }
      }
      .padding(.horizontal, 16)
      .padding(.bottom, 28)
    }
    .task {
      await store.refreshStatus()
    }
    .fileImporter(
      isPresented: $showAddFiles,
      allowedContentTypes: [.item, .data],
      allowsMultipleSelection: true
    ) { result in
      handlePickedFiles(result: result)
    }
    .fileImporter(
      isPresented: $showImportZip,
      allowedContentTypes: [.zip],
      allowsMultipleSelection: false
    ) { result in
      handleImportZip(result: result)
    }
    .fileExporter(
      isPresented: $showExporter,
      document: exportDoc,
      contentType: .zip,
      defaultFilename: "civicshield_vault"
    ) { _ in
      // No-op. Export errors are surfaced by SwiftUI automatically.
    }
    .alert("Delete vault?", isPresented: $showDeleteVaultConfirm) {
      Button("Delete", role: .destructive) {
        Task {
          do {
            try await store.deleteVault()
          } catch {
            self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to delete vault."
          }
        }
      }
      Button("Cancel", role: .cancel) {}
    } message: {
      Text("This removes the local encrypted vault from this device. This cannot be undone.")
    }
  }

  @ViewBuilder
  private func lockedSection(hasVault: Bool) -> some View {
    GlassCard {
      HStack(alignment: .firstTextBaseline) {
        Text(hasVault ? "Vault locked" : "No vault yet")
          .font(.system(.headline, design: .rounded).weight(.bold))
        Spacer()
        StatusPill(text: hasVault ? "Locked" : "Empty", tint: hasVault ? .gray : .orange)
      }

      if hasVault {
        Text("Enter your passphrase to unlock. If you forgot it, you can only delete and recreate the vault.")
          .font(.system(.subheadline, design: .rounded))
          .foregroundStyle(.secondary)
      } else {
        Text("Create a new vault with a passphrase. Keep it safe. Without it, the vault can't be unlocked.")
          .font(.system(.subheadline, design: .rounded))
          .foregroundStyle(.secondary)
      }

      VStack(alignment: .leading, spacing: 10) {
        SecureField("Passphrase", text: $passphrase)
          .textInputAutocapitalization(.never)
          .autocorrectionDisabled()
          .padding(12)
          .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))

        if !hasVault {
          SecureField("Confirm passphrase", text: $confirmPassphrase)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .padding(12)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
      }

      Group {
        if hasVault {
          Button {
            Task { await unlock() }
          } label: {
            Label("Unlock", systemImage: "lock.open")
          }
          .buttonStyle(PrimaryButtonStyle(tint: AppTab.shield.accent))
        } else {
          Button {
            Task { await create() }
          } label: {
            Label("Create vault", systemImage: "lock.shield")
          }
          .buttonStyle(PrimaryButtonStyle(tint: AppTab.shield.accent))
        }
      }
      .disabled(store.busy)

      Button {
        // Import replaces local vault only after successful decrypt + parse.
        error = nil
        showImportZip = true
      } label: {
        Label("Import vault zip", systemImage: "square.and.arrow.down")
      }
      .buttonStyle(TonalButtonStyle(tint: AppTab.shield.accent))
      .disabled(store.busy)

      if hasVault {
        Button(role: .destructive) {
          showDeleteVaultConfirm = true
        } label: {
          Label("Delete local vault", systemImage: "trash")
        }
        .buttonStyle(TonalButtonStyle(tint: .gray))
        .disabled(store.busy)
      }

      if store.busy {
        HStack(spacing: 10) {
          ProgressView()
          Text("Working...")
            .font(.system(.subheadline, design: .rounded).weight(.semibold))
            .foregroundStyle(.secondary)
        }
      }
    }
  }

  @ViewBuilder
  private func unlockedSection(unlocked: VaultUnlocked) -> some View {
    GlassCard {
      HStack(alignment: .firstTextBaseline) {
        VStack(alignment: .leading, spacing: 6) {
          Text("Vault unlocked")
            .font(.system(.headline, design: .rounded).weight(.bold))
          Text("Items: \(unlocked.items.count)")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)
        }
        Spacer()
        StatusPill(text: "Unlocked", tint: AppTab.shield.accent)
      }

      HStack(spacing: 12) {
        Button {
          error = nil
          showAddFiles = true
        } label: {
          Label("Add files", systemImage: "plus")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(PrimaryButtonStyle(tint: AppTab.shield.accent))
        .disabled(store.busy)

        Button {
          Task { await export() }
        } label: {
          Label("Export zip", systemImage: "square.and.arrow.up")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(TonalButtonStyle(tint: AppTab.shield.accent))
        .disabled(store.busy)
      }

      HStack(spacing: 12) {
        Button {
          store.lock()
          passphrase = ""
          confirmPassphrase = ""
          error = nil
        } label: {
          Label("Lock", systemImage: "lock")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(TonalButtonStyle(tint: .gray))
        .disabled(store.busy)

        Button(role: .destructive) {
          showDeleteVaultConfirm = true
        } label: {
          Label("Delete vault", systemImage: "trash")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(TonalButtonStyle(tint: .gray))
        .disabled(store.busy)
      }

      if store.busy {
        HStack(spacing: 10) {
          ProgressView()
          Text("Working...")
            .font(.system(.subheadline, design: .rounded).weight(.semibold))
            .foregroundStyle(.secondary)
        }
      }
    }

    if !unlocked.items.isEmpty {
      LazyVStack(spacing: 12) {
        ForEach(unlocked.items) { item in
          VaultItemCard(item: item, onDelete: {
            Task {
              do {
                try await store.deleteItem(id: item.id)
              } catch {
                self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to delete item."
              }
            }
          })
        }
      }
    } else {
      GlassCard {
        Text("No items yet.")
          .font(.system(.subheadline, design: .rounded).weight(.semibold))
        Text("Add files to include receipts, PDFs, screenshots, or other evidence. Everything stays encrypted on-device.")
          .font(.system(.subheadline, design: .rounded))
          .foregroundStyle(.secondary)
      }
    }
  }

  private func create() async {
    error = nil
    let p = passphrase.trimmingCharacters(in: .whitespacesAndNewlines)
    let c = confirmPassphrase.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !p.isEmpty else {
      error = "Enter a passphrase."
      return
    }
    guard p == c else {
      error = "Passphrases do not match."
      return
    }
    do {
      try await store.createNew(passphrase: p)
      passphrase = ""
      confirmPassphrase = ""
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to create vault."
    }
  }

  private func unlock() async {
    error = nil
    let p = passphrase.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !p.isEmpty else {
      error = "Enter a passphrase."
      return
    }
    do {
      try await store.unlock(passphrase: p)
      passphrase = ""
      confirmPassphrase = ""
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to unlock."
    }
  }

  private func export() async {
    error = nil
    do {
      let data = try await store.exportZip()
      exportDoc = DataDocument(data: data)
      showExporter = true
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to export."
    }
  }

  private func handlePickedFiles(result: Result<[URL], Error>) {
    switch result {
    case let .success(urls):
      guard !urls.isEmpty else { return }
      Task {
        do {
          let scoped: [(URL, Bool)] = urls.map { ($0, $0.startAccessingSecurityScopedResource()) }
          defer {
            for (u, ok) in scoped where ok { u.stopAccessingSecurityScopedResource() }
          }
          try await store.addFiles(urls: urls)
        } catch {
          self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to add files."
        }
      }
    case let .failure(err):
      self.error = err.localizedDescription
    }
  }

  private func handleImportZip(result: Result<[URL], Error>) {
    switch result {
    case let .success(urls):
      guard let url = urls.first else { return }
      Task {
        do {
          let ok = url.startAccessingSecurityScopedResource()
          defer { if ok { url.stopAccessingSecurityScopedResource() } }

          let data = try Data(contentsOf: url)
          let p = passphrase.trimmingCharacters(in: .whitespacesAndNewlines)
          guard !p.isEmpty else {
            error = "Enter the vault passphrase to import."
            return
          }
          try await store.importZip(passphrase: p, zipData: data)
          passphrase = ""
          confirmPassphrase = ""
        } catch {
          self.error = (error as? LocalizedError)?.errorDescription ?? "Unable to import."
        }
      }
    case let .failure(err):
      self.error = err.localizedDescription
    }
  }
}

private struct VaultItemCard: View {
  let item: VaultItemMeta
  let onDelete: () -> Void

  var body: some View {
    GlassCard {
      HStack(alignment: .firstTextBaseline, spacing: 10) {
        Text(item.originalName)
          .font(.system(.headline, design: .rounded).weight(.bold))
        Spacer()
        Button(role: .destructive, action: onDelete) {
          Image(systemName: "trash")
            .font(.system(size: 14, weight: .semibold))
        }
        .buttonStyle(.plain)
      }

      Text(item.contentType)
        .font(.system(.caption, design: .rounded))
        .foregroundStyle(.secondary)

      Text("\(item.size) bytes")
        .font(.system(.caption, design: .rounded))
        .foregroundStyle(.secondary)
    }
  }
}
