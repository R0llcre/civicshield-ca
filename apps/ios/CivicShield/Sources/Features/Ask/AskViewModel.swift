import Foundation

@MainActor
final class AskViewModel: ObservableObject {
  @Published var input: String = ""
  @Published var sending: Bool = false
  @Published var error: String? = nil
  @Published var messages: [AskMessage] = []
  @Published var hasConsented: Bool = false

  private let storageKey = "ask.messages.v1"
  private let consentKey = "ask.consent.v1"

  private let encoder: JSONEncoder = {
    let e = JSONEncoder()
    e.dateEncodingStrategy = .iso8601
    return e
  }()

  private let decoder: JSONDecoder = {
    let d = JSONDecoder()
    d.dateDecodingStrategy = .iso8601
    return d
  }()

  init() {
    self.hasConsented = UserDefaults.standard.bool(forKey: consentKey)
    load()
  }

  func clear() {
    messages = []
    error = nil
    input = ""
    save()
  }

  func setConsented() {
    hasConsented = true
    UserDefaults.standard.set(true, forKey: consentKey)
  }

  func send(api: ApiClient, locale: String = "en") async {
    let q = input.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !q.isEmpty else {
      error = "Enter a question."
      return
    }

    sending = true
    error = nil

    let userMsg = AskMessage(role: "user", content: q)
    messages.append(userMsg)
    input = ""
    trimAndSave()

    do {
      let apiMsgs = buildApiMessages(from: messages)
      let res: AskResponse = try await api.post(
        "/api/ask",
        body: AskRequest(locale: locale, messages: apiMsgs),
        timeout: 60
      )

      let assistant = AskMessage(
        role: "assistant",
        content: res.answerMarkdown,
        citations: res.citations,
        suggestedActions: res.suggestedActions
      )
      messages.append(assistant)
      trimAndSave()
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "AI request failed. Please try again."
      trimAndSave()
    }

    sending = false
  }

  private func buildApiMessages(from msgs: [AskMessage]) -> [AskApiMessage] {
    // Keep the most recent turns; do not include any non-user/assistant roles.
    let cleaned = msgs
      .map { AskApiMessage(role: $0.role.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), content: $0.content) }
      .filter { ($0.role == "user" || $0.role == "assistant") && !$0.content.isEmpty }

    let tail = cleaned.suffix(20)
    // Hard cap payload size (chars) to avoid server 400.
    var out: [AskApiMessage] = []
    var total = 0
    for m in tail {
      let c = m.content
      if c.count > 2000 { continue }
      if total + c.count > 8000 { break }
      out.append(m)
      total += c.count
    }
    return out
  }

  private func trimAndSave() {
    if messages.count > 50 {
      messages = Array(messages.suffix(50))
    }
    save()
  }

  private func load() {
    guard let data = UserDefaults.standard.data(forKey: storageKey) else { return }
    if let decoded = try? decoder.decode([AskMessage].self, from: data) {
      self.messages = decoded
    }
  }

  private func save() {
    if let data = try? encoder.encode(messages) {
      UserDefaults.standard.set(data, forKey: storageKey)
    }
  }
}

