import SwiftUI

struct AskView: View {
  @EnvironmentObject private var env: AppEnvironment
  @StateObject private var vm = AskViewModel()
  @Environment(\.openURL) private var openURL
  @State private var showConsent = false

  var body: some View {
    ScrollViewReader { proxy in
      ScrollView {
        VStack(alignment: .leading, spacing: 14) {
          HStack {
            Spacer()
            Button("Clear chat") { vm.clear() }
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
              .foregroundStyle(AppTab.ask.accent)
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
            ForEach(vm.messages) { m in
              ChatBubble(message: m)
                .id(m.id)
            }
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
                Task {
                  await vm.send(api: env.api, locale: "en")
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
            }
          }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 28)
      }
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

    var isUser: Bool { message.role.lowercased() == "user" }

    var body: some View {
      VStack(alignment: isUser ? .trailing : .leading, spacing: 8) {
        Group {
          if isUser {
            Text(message.content)
              .font(.system(.body, design: .rounded))
              .foregroundStyle(.white)
          } else {
            MarkdownView(
              markdown: message.content,
              accent: UIColor(AppTab.ask.accent),
              onOpenURL: { url in
                openURL(url)
              }
            )
          }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)
        .background(
          isUser ? AppTab.ask.accent : Color.white.opacity(0.65),
          in: RoundedRectangle(cornerRadius: 22, style: .continuous)
        )
        .overlay(
          RoundedRectangle(cornerRadius: 22, style: .continuous)
            .stroke(Color.white.opacity(isUser ? 0.18 : 0.35), lineWidth: 1)
        )

        if !isUser {
          if let actions = message.suggestedActions, !actions.isEmpty {
            ActionChips(actions: actions)
          }
          if let citations = message.citations, !citations.isEmpty {
            CitationsList(citations: citations)
          }
        }
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
