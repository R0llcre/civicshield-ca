import SwiftUI

struct AskView: View {
  @EnvironmentObject private var env: AppEnvironment
  @StateObject private var vm = AskViewModel()
  @Environment(\.openURL) private var openURL
  @State private var showConsent = false
  @State private var showThreads = false
  @State private var showExport = false
  @State private var exportText: String = ""
  @FocusState private var inputFocused: Bool

  var body: some View {
    ScrollViewReader { proxy in
      ScrollView {
        LazyVStack(alignment: .leading, spacing: 14) {
          HStack {
            Button("Chats") { showThreads = true }
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
              .foregroundStyle(AppTab.ask.accent)

            Button("New") { vm.newChat() }
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
              .foregroundStyle(AppTab.ask.accent)

            Spacer()

            Menu {
              Button("Export") {
                exportText = vm.exportText()
                showExport = true
              }
              Button("Clear this chat") { vm.clear() }
              if let tid = vm.activeThreadId {
                Button("Delete this chat", role: .destructive) {
                  vm.deleteThread(tid)
                }
              }
            } label: {
              Image(systemName: "ellipsis.circle")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(AppTab.ask.accent)
            }
          }

          GlassCard {
            Text("Ask (AI Q&A)")
              .font(.system(.title, design: .rounded).weight(.bold))
              .foregroundStyle(Color(red: 0.06, green: 0.11, blue: 0.23))
            Text("Information-only Q&A about CA immigration basics, legal aid, and what to verify next.")
              .font(.system(.subheadline, design: .rounded))
              .foregroundStyle(.secondary)
          }

          if vm.messages.isEmpty {
            GlassCard {
              Text("Ask a question to get started. Your chat stays on this device unless you clear it.")
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(.secondary)
            }
          } else {
            MessagesTimeline(messages: vm.messages)
          }

          GlassCard {
            VStack(alignment: .leading, spacing: 10) {
              Text("Your question")
                .font(.system(.caption, design: .rounded).weight(.semibold))
                .foregroundStyle(.secondary)

              TextField("Ask about immigration steps, deadlines, legal aid options, or how to verify credentials...", text: $vm.input, axis: .vertical)
                .lineLimit(3...6)
                .textInputAutocapitalization(.sentences)
                .autocorrectionDisabled()
                .focused($inputFocused)
                .padding(12)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))

              if let err = vm.error {
                Text(err)
                  .font(.system(.subheadline, design: .rounded).weight(.semibold))
                  .foregroundStyle(.red)
              }

              Button {
                if !vm.hasConsented {
                  showConsent = true
                  return
                }
                inputFocused = false
                Task {
                  await vm.send(api: env.api, locale: deviceLocale())
                  if let last = vm.messages.last {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                      proxy.scrollTo(last.id, anchor: .bottom)
                    }
                  }
                }
              } label: {
                HStack(spacing: 10) {
                  Image(systemName: "paperplane.fill")
                  Text(vm.sending ? "Sending..." : "Send")
                }
              }
              .buttonStyle(PrimaryButtonStyle(tint: AppTab.ask.accent))
              .disabled(vm.sending)

              Text("Information only. Not legal advice.")
                .font(.system(.caption, design: .rounded))
                .foregroundStyle(.secondary)
              Text("Long chats may send only recent context.")
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(.secondary)
            }
          }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 28)
      }
      .onAppear {
        PerfSignpost.event("ask_view_appear")
        PerfSignpost.event("ask_view_prewarm_check")
        WebViewPrewarmer.shared.prewarmIfNeeded()
      }
      .scrollDismissesKeyboard(.interactively)
      .toolbar {
        ToolbarItemGroup(placement: .keyboard) {
          Spacer()
          Button("Done") { inputFocused = false }
        }
      }
      .simultaneousGesture(
        TapGesture().onEnded {
          inputFocused = false
        }
      )
      .sheet(isPresented: $showConsent) {
        ConsentSheet(
          onAgree: {
            vm.setConsented()
            showConsent = false
          },
          onCancel: {
            showConsent = false
          }
        )
        .presentationDetents([.medium])
      }
      .sheet(isPresented: $showThreads) {
        ThreadsSheet(
          threads: vm.threads,
          activeThreadId: vm.activeThreadId,
          onNew: {
            vm.newChat()
            showThreads = false
          },
          onSelect: { id in
            vm.setActiveThread(id)
            showThreads = false
          },
          onRename: { id, title in
            vm.renameThread(id, title: title)
          },
          onDelete: { id in
            vm.deleteThread(id)
          }
        )
        .presentationDetents([.medium, .large])
      }
      .sheet(isPresented: $showExport) {
        ActivityView(items: [exportText])
      }
    }
  }

  private func deviceLocale() -> String {
    // Normalize to the 3 supported locales on the server.
    let raw = (Locale.preferredLanguages.first ?? "en").lowercased()
    if raw.hasPrefix("es") { return "es" }
    if raw.hasPrefix("zh") { return "zh-Hans" }
    return "en"
  }

  private struct MessagesTimeline: View {
    let messages: [AskMessage]

    var body: some View {
      ForEach(messages) { m in
        ChatBubble(message: m)
          .id(m.id)
      }
    }
  }

  private struct ThreadsSheet: View {
    let threads: [AskThreadMeta]
    let activeThreadId: String?
    let onNew: () -> Void
    let onSelect: (String) -> Void
    let onRename: (String, String) -> Void
    let onDelete: (String) -> Void

    @State private var q: String = ""
    @State private var renamingId: String? = nil
    @State private var renameDraft: String = ""
    @State private var deleteId: String? = nil
    @State private var confirmDelete = false

    private var filteredThreads: [AskThreadMeta] {
      let query = q.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
      if query.isEmpty { return threads }
      return threads.filter {
        $0.title.lowercased().contains(query) || $0.lastPreview.lowercased().contains(query)
      }
    }

    var body: some View {
      NavigationView {
        VStack(spacing: 12) {
          HStack(spacing: 12) {
            TextField("Search chats", text: $q)
              .textInputAutocapitalization(.never)
              .autocorrectionDisabled()
              .padding(12)
              .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            Button("New", action: onNew)
              .buttonStyle(PrimaryButtonStyle(tint: AppTab.ask.accent))
          }
          .padding(.horizontal, 16)
          .padding(.top, 16)

          List {
            if filteredThreads.isEmpty {
              Text("No chats match your search.")
                .foregroundStyle(.secondary)
            }

            ForEach(filteredThreads) { t in
              threadRow(t)
            }
          }
          .listStyle(.plain)
        }
        .navigationTitle("Chats")
        .alert("Delete chat?", isPresented: $confirmDelete) {
          Button("Cancel", role: .cancel) { confirmDelete = false }
          Button("Delete", role: .destructive) {
            if let id = deleteId { onDelete(id) }
            confirmDelete = false
          }
        } message: {
          Text("This deletes the chat from this device. This can't be undone.")
        }
      }
    }

    @ViewBuilder
    private func threadRow(_ t: AskThreadMeta) -> some View {
      VStack(alignment: .leading, spacing: 10) {
        if renamingId == t.id {
          VStack(alignment: .leading, spacing: 10) {
            TextField("Chat title", text: $renameDraft)
              .padding(10)
              .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            HStack(spacing: 12) {
              Button("Save") {
                let trimmed = renameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty { onRename(t.id, trimmed) }
                renamingId = nil
                renameDraft = ""
              }
              .buttonStyle(PrimaryButtonStyle(tint: AppTab.ask.accent))
              Button("Cancel") {
                renamingId = nil
                renameDraft = ""
              }
              .buttonStyle(TonalButtonStyle(tint: .gray))
            }
          }
        } else {
          VStack(alignment: .leading, spacing: 4) {
            Text(t.title)
              .font(.system(.headline, design: .rounded).weight(t.id == activeThreadId ? .bold : .semibold))
            if !t.lastPreview.isEmpty {
              Text(t.lastPreview)
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(.secondary)
                .lineLimit(2)
            }
            Text("\(t.messageCount) msgs · \(t.updatedAt.formatted(date: .abbreviated, time: .shortened))")
              .font(.system(.caption, design: .rounded))
              .foregroundStyle(.secondary)
          }

          HStack(spacing: 10) {
            if t.id == activeThreadId {
              Button("Active") { onSelect(t.id) }
                .buttonStyle(TonalButtonStyle(tint: .gray))
            } else {
              Button("Open") { onSelect(t.id) }
                .buttonStyle(PrimaryButtonStyle(tint: AppTab.ask.accent))
            }
            Button {
              renamingId = t.id
              renameDraft = t.title
            } label: {
              Image(systemName: "pencil")
            }
            .buttonStyle(TonalButtonStyle(tint: .gray))
            Button(role: .destructive) {
              deleteId = t.id
              confirmDelete = true
            } label: {
              Image(systemName: "trash")
            }
            .buttonStyle(TonalButtonStyle(tint: .gray))
          }
        }
      }
      .listRowBackground(Color.clear)
    }
  }

  private struct ConsentSheet: View {
    let onAgree: () -> Void
    let onCancel: () -> Void

    var body: some View {
      VStack(alignment: .leading, spacing: 16) {
        Text("Before you ask")
          .font(.system(.title3, design: .rounded).weight(.bold))
        Text("I understand this is general information, not legal advice, and I should not share sensitive personal information.")
          .font(.system(.body, design: .rounded))
          .foregroundStyle(.secondary)

        HStack(spacing: 12) {
          Button("Cancel", action: onCancel)
            .buttonStyle(TonalButtonStyle(tint: .gray))
          Button("I understand", action: onAgree)
            .buttonStyle(PrimaryButtonStyle(tint: AppTab.ask.accent))
        }
      }
      .padding(20)
    }
  }

 private struct ChatBubble: View {
    @EnvironmentObject private var env: AppEnvironment
    @Environment(\.openURL) private var openURL
    let message: AskMessage
    @State private var richTextActivated = false
    @State private var activationTask: Task<Void, Never>? = nil

    var isUser: Bool { message.role.lowercased() == "user" }
    private let bubbleMaxWidth: CGFloat = 520

    var body: some View {
      let shape = RoundedRectangle(cornerRadius: 22, style: .continuous)
      HStack(alignment: .bottom, spacing: 0) {
        if isUser { Spacer(minLength: 32) }

        VStack(alignment: isUser ? .trailing : .leading, spacing: 8) {
          Group {
            if isUser {
              Text(message.content)
                .font(.system(.body, design: .rounded))
                .foregroundStyle(.white)
                .multilineTextAlignment(.leading)
            } else {
              if richTextActivated {
                MarkdownView(
                  markdown: message.content,
                  accent: UIColor(AppTab.ask.accent),
                  onOpenURL: { url in
                    openURL(url)
                  }
                )
              } else {
                VStack(alignment: .leading, spacing: 8) {
                  HStack(spacing: 8) {
                    ProgressView()
                      .controlSize(.small)
                    Text("Rendering response...")
                      .font(.system(.subheadline, design: .rounded).weight(.semibold))
                      .foregroundStyle(.secondary)
                  }

                  VStack(alignment: .leading, spacing: 6) {
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                      .fill(Color.primary.opacity(0.10))
                      .frame(height: 11)
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                      .fill(Color.primary.opacity(0.08))
                      .frame(height: 11)
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                      .fill(Color.primary.opacity(0.06))
                      .frame(width: 180, height: 11)
                  }
                }
              }
            }
          }
          .padding(14)
          // Important: apply the width constraint *after* the background so short user messages
          // don't paint a full-width accent bar.
          .background(
            isUser ? AnyShapeStyle(AppTab.ask.accent) : AnyShapeStyle(.thinMaterial),
            in: shape
          )
          .overlay(
            Group {
              if !isUser {
                shape.fill(
                  LinearGradient(
                    colors: [AppTab.ask.accent.opacity(0.08), .clear],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                  )
                )
              }
            }
          )
          .overlay(
            Group {
              if isUser {
                shape.stroke(Color.white.opacity(0.18), lineWidth: 1)
              } else {
                shape.stroke(
                  LinearGradient(
                    colors: [
                      .white.opacity(0.95),
                      AppTab.ask.accent.opacity(0.22),
                      .white.opacity(0.55),
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                  ),
                  lineWidth: 1
                )
              }
            }
          )
          .shadow(color: .black.opacity(isUser ? 0.0 : 0.06), radius: 12, x: 0, y: 8)
          .frame(maxWidth: bubbleMaxWidth, alignment: isUser ? .trailing : .leading)

          if !isUser {
            if let actions = message.suggestedActions, !actions.isEmpty {
              ActionChips(actions: actions)
                .frame(maxWidth: bubbleMaxWidth, alignment: .leading)
            }
            if let citations = message.citations, !citations.isEmpty {
              CitationsList(citations: citations)
                .frame(maxWidth: bubbleMaxWidth, alignment: .leading)
            }
          }
        }
        .onAppear {
          guard !isUser else { return }
          guard !richTextActivated else { return }
          activationTask?.cancel()
          activationTask = Task {
            try? await Task.sleep(nanoseconds: 120_000_000)
            guard !Task.isCancelled else { return }
            richTextActivated = true
          }
        }
        .onDisappear {
          activationTask?.cancel()
          activationTask = nil
        }

        if !isUser { Spacer(minLength: 32) }
      }
    }

    @ViewBuilder
    private func ActionChips(actions: [AskSuggestedAction]) -> some View {
      VStack(alignment: .leading, spacing: 8) {
        Text("Suggested next steps")
          .font(.system(.caption, design: .rounded).weight(.semibold))
          .foregroundStyle(.secondary)

        let cols = [GridItem(.adaptive(minimum: 160), spacing: 8, alignment: .leading)]
        LazyVGrid(columns: cols, alignment: .leading, spacing: 8) {
          ForEach(actions) { a in
            Button {
              handleAction(a)
            } label: {
              HStack(spacing: 6) {
                Image(systemName: iconForAction(a.type))
                  .font(.system(size: 12, weight: .semibold))
                Text(a.label)
                  .font(.system(.caption, design: .rounded).weight(.semibold))
                  .lineLimit(1)
              }
              .padding(.horizontal, 10)
              .padding(.vertical, 8)
              .frame(maxWidth: .infinity, alignment: .leading)
              .background(AppTab.ask.accent.opacity(0.10), in: Capsule())
              .overlay(Capsule().stroke(AppTab.ask.accent.opacity(0.18), lineWidth: 1))
            }
            .buttonStyle(.plain)
          }
        }
      }
    }

    @ViewBuilder
    private func CitationsList(citations: [AskCitation]) -> some View {
      VStack(alignment: .leading, spacing: 8) {
        Text("Official sources")
          .font(.system(.caption, design: .rounded).weight(.semibold))
          .foregroundStyle(.secondary)

        ForEach(citations.prefix(6)) { c in
          Button {
            if let u = URL(string: c.url), UrlPolicy.isAllowed(u) {
              openURL(u)
            }
          } label: {
            HStack(alignment: .top, spacing: 10) {
              Image(systemName: "arrow.up.right.square")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(AppTab.ask.accent)
              VStack(alignment: .leading, spacing: 2) {
                Text(c.title.isEmpty ? c.url : c.title)
                  .font(.system(.subheadline, design: .rounded).weight(.semibold))
                Text(c.url)
                  .font(.system(.caption2, design: .rounded))
                  .foregroundStyle(.secondary)
                  .lineLimit(1)
              }
              Spacer(minLength: 0)
            }
            .padding(10)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
          }
          .buttonStyle(.plain)
        }
      }
    }

    private func iconForAction(_ type: String) -> String {
      switch type {
      case "open_verify": return "checkmark.seal"
      case "open_help": return "lifepreserver"
      case "open_official_url": return "arrow.up.right.square"
      default: return "sparkles"
      }
    }

    private func handleAction(_ a: AskSuggestedAction) {
      switch a.type {
      case "open_verify":
        env.tab = .verify
      case "open_help":
        env.tab = .help
      case "open_official_url":
        if let raw = a.payload?["url"], let u = URL(string: raw), UrlPolicy.isAllowed(u) {
          openURL(u)
        }
      default:
        break
      }
    }
  }
}
