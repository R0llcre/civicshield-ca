import Foundation

@MainActor
final class AskViewModel: ObservableObject {
  @Published var input: String = ""
  @Published var sending: Bool = false
  @Published var error: String? = nil
  @Published var messages: [AskMessage] = []
  @Published var threads: [AskThreadMeta] = []
  @Published var activeThreadId: String? = nil
  @Published var hasConsented: Bool = false

  private let ud: UserDefaults

  private let consentKey = "ask.consent.v1"
  private let threadsIndexKey = "ask.threads.index.v2"
  private func threadKey(_ id: String) -> String { "ask.thread.\(id).v2" }
  private let legacyMessagesKey = "ask.messages.v1"

  private let maxThreads = 30
  private let maxMessagesPerThread = 50
  private let maxPreviewChars = 80
  private let maxTitleChars = 48

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

  init(userDefaults: UserDefaults = .standard) {
    self.ud = userDefaults
    self.hasConsented = ud.bool(forKey: consentKey)
    migrateIfNeeded()
    loadIndexAndActive()
  }

  func clear() {
    // Clear current thread content (not all threads).
    guard let tid = activeThreadId else { return }
    messages = []
    error = nil
    input = ""
    saveActiveThreadMessages(threadId: tid, messages: [])
    updateMeta(threadId: tid, messages: [])
    persistIndex()
  }

  func newChat(title: String = "New chat") {
    let id = UUID().uuidString
    let now = Date()
    let beforeIds = Set(threads.map { $0.id })
    let meta = AskThreadMeta(
      id: id,
      title: String(title.prefix(maxTitleChars)),
      createdAt: now,
      updatedAt: now,
      messageCount: 0,
      lastPreview: ""
    )
    threads.append(meta)
    let normalized = normalizeThreads(threads)
    let afterIds = Set(normalized.map { $0.id })
    let evicted = beforeIds.subtracting(afterIds)
    for tid in evicted {
      ud.removeObject(forKey: threadKey(tid))
    }
    threads = normalized
    activeThreadId = id
    messages = []
    saveActiveThreadMessages(threadId: id, messages: [])
    persistIndex()
  }

  func setActiveThread(_ id: String) {
    guard threads.contains(where: { $0.id == id }) else { return }
    activeThreadId = id
    messages = loadThreadMessages(threadId: id)
    error = nil
    persistIndex()
  }

  func renameThread(_ id: String, title: String) {
    let t = title.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !t.isEmpty else { return }
    let now = Date()
    threads = threads.map { m in
      if m.id == id {
        return AskThreadMeta(
          id: m.id,
          title: String(t.prefix(maxTitleChars)),
          createdAt: m.createdAt,
          updatedAt: now,
          messageCount: m.messageCount,
          lastPreview: m.lastPreview
        )
      }
      return m
    }
    persistIndex()
  }

  func deleteThread(_ id: String) {
    threads.removeAll { $0.id == id }
    ud.removeObject(forKey: threadKey(id))
    if activeThreadId == id {
      activeThreadId = threads.last?.id
      if let tid = activeThreadId {
        messages = loadThreadMessages(threadId: tid)
      } else {
        messages = []
      }
    }
    if threads.isEmpty {
      // Keep Ask usable: create a fresh empty chat.
      newChat(title: "Chat 1")
      return
    }
    persistIndex()
  }

  func exportText() -> String {
    let title = threads.first(where: { $0.id == activeThreadId })?.title ?? "Chat"
    var out: [String] = []
    out.append(title)
    out.append("Information only. Not legal advice.")
    out.append("")
    for m in messages {
      let role = m.role.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
      out.append("[\(iso(m.createdAt))] \(role):")
      out.append(m.content.trimmingCharacters(in: .whitespacesAndNewlines))
      out.append("")
      if role == "assistant", let citations = m.citations, !citations.isEmpty {
        out.append("Sources:")
        for c in citations.prefix(6) {
          out.append("- \(c.title): \(c.url)")
        }
        out.append("")
      }
    }
    let joined = out.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
    if joined.count > 120_000 {
      let idx = joined.index(joined.startIndex, offsetBy: 120_000)
      return String(joined[..<idx]) + "\n\n[Truncated]\n"
    }
    return joined
  }

  func setConsented() {
    hasConsented = true
    ud.set(true, forKey: consentKey)
  }

  func send(api: ApiClient, locale: String = "en") async {
    let q = input.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !q.isEmpty else {
      error = "Enter a question."
      return
    }

    if activeThreadId == nil {
      newChat(title: "Chat 1")
    }
    guard let tid = activeThreadId else { return }

    sending = true
    error = nil

    let userMsg = AskMessage(role: "user", content: q)
    messages.append(userMsg)
    input = ""
    trimAndSave(threadId: tid)

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
      trimAndSave(threadId: tid)
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "AI request failed. Please try again."
      trimAndSave(threadId: tid)
    }

    sending = false
  }

  private func buildApiMessages(from msgs: [AskMessage]) -> [AskApiMessage] {
    // Keep the most recent turns; do not include any non-user/assistant roles.
    let cleaned = msgs
      .map { AskApiMessage(role: $0.role.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), content: $0.content) }
      .filter { ($0.role == "user" || $0.role == "assistant") && !$0.content.isEmpty }

    let tail = cleaned.suffix(20)
    // Hard cap payload size (chars) to avoid server 400. Keep the most recent content first.
    var out: [AskApiMessage] = []
    var total = 0
    for m in tail.reversed() {
      let role = m.role
      var c = m.content
      if c.count > 2000 {
        // Keep the most recent part of very long messages.
        c = String(c.suffix(2000))
      }
      if total + c.count > 8000 {
        let remaining = 8000 - total
        if remaining < 200 { break }
        c = String(c.suffix(remaining))
      }
      out.append(AskApiMessage(role: role, content: c))
      total += c.count
      if total >= 8000 { break }
    }
    return out.reversed()
  }

  private func trimAndSave(threadId: String) {
    if messages.count > maxMessagesPerThread {
      messages = Array(messages.suffix(maxMessagesPerThread))
    }
    saveActiveThreadMessages(threadId: threadId, messages: messages)
    updateMeta(threadId: threadId, messages: messages)
    persistIndex()
  }

  private func iso(_ d: Date) -> String {
    let f = ISO8601DateFormatter()
    f.timeZone = TimeZone(secondsFromGMT: 0)
    return f.string(from: d)
  }

  private func loadIndexAndActive() {
    if let data = ud.data(forKey: threadsIndexKey),
       let idx = try? decoder.decode(AskThreadsIndex.self, from: data),
       idx.version == 2 {
      self.threads = idx.threads
      self.activeThreadId = idx.activeThreadId ?? idx.threads.last?.id
    }
    if threads.isEmpty {
      newChat(title: "Chat 1")
      return
    }
    if let tid = activeThreadId {
      self.messages = loadThreadMessages(threadId: tid)
    }
  }

  private func persistIndex() {
    let idx = AskThreadsIndex(version: 2, activeThreadId: activeThreadId, threads: threads)
    if let data = try? encoder.encode(idx) {
      ud.set(data, forKey: threadsIndexKey)
    }
  }

  private func loadThreadMessages(threadId: String) -> [AskMessage] {
    guard let data = ud.data(forKey: threadKey(threadId)),
          let decoded = try? decoder.decode([AskMessage].self, from: data) else {
      return []
    }
    return decoded
  }

  private func saveActiveThreadMessages(threadId: String, messages: [AskMessage]) {
    if let data = try? encoder.encode(messages) {
      ud.set(data, forKey: threadKey(threadId))
    }
  }

  private func updateMeta(threadId: String, messages: [AskMessage]) {
    let now = Date()
    let preview = messages.last.map { condense($0.content).prefix(maxPreviewChars) } ?? ""
    let messageCount = messages.count
    threads = threads.map { m in
      guard m.id == threadId else { return m }
      let title = m.title.isEmpty ? deriveTitle(from: messages) : m.title
      return AskThreadMeta(
        id: m.id,
        title: String(title.prefix(maxTitleChars)),
        createdAt: m.createdAt,
        updatedAt: now,
        messageCount: messageCount,
        lastPreview: String(preview)
      )
    }
  }

  private func deriveTitle(from messages: [AskMessage]) -> String {
    if let firstUser = messages.first(where: { $0.role.lowercased() == "user" })?.content {
      let t = condense(firstUser)
      if !t.isEmpty { return String(t.prefix(maxTitleChars)) }
    }
    return "Chat 1"
  }

  private func condense(_ s: String) -> String {
    s.replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression).trimmingCharacters(in: .whitespacesAndNewlines)
  }

  private func normalizeThreads(_ t: [AskThreadMeta]) -> [AskThreadMeta] {
    let sorted = t.sorted(by: { $0.createdAt < $1.createdAt })
    if sorted.count <= maxThreads { return sorted }
    return Array(sorted.suffix(maxThreads))
  }

  private func migrateIfNeeded() {
    // Already migrated?
    if ud.data(forKey: threadsIndexKey) != nil { return }
    guard let legacy = ud.data(forKey: legacyMessagesKey),
          let legacyMsgs = try? decoder.decode([AskMessage].self, from: legacy),
          !legacyMsgs.isEmpty else {
      return
    }

    let id = UUID().uuidString
    if let data = try? encoder.encode(legacyMsgs) {
      ud.set(data, forKey: threadKey(id))
    }

    let title = deriveTitle(from: legacyMsgs)
    let meta = AskThreadMeta(
      id: id,
      title: title,
      createdAt: legacyMsgs.first?.createdAt ?? Date(),
      updatedAt: legacyMsgs.last?.createdAt ?? Date(),
      messageCount: legacyMsgs.count,
      lastPreview: String(condense(legacyMsgs.last?.content ?? "").prefix(maxPreviewChars))
    )
    let idx = AskThreadsIndex(version: 2, activeThreadId: id, threads: [meta])
    if let data = try? encoder.encode(idx) {
      ud.set(data, forKey: threadsIndexKey)
    }
    ud.removeObject(forKey: legacyMessagesKey)
  }
}

// MARK: - Threads model

struct AskThreadMeta: Codable, Identifiable {
  let id: String
  let title: String
  let createdAt: Date
  let updatedAt: Date
  let messageCount: Int
  let lastPreview: String
}

struct AskThreadsIndex: Codable {
  let version: Int
  let activeThreadId: String?
  let threads: [AskThreadMeta]
}
