import Foundation

/// Serializes markdown -> HTML render work to prevent CPU spikes when many
/// assistant bubbles appear at once.
actor MarkdownRenderScheduler {
  static let shared = MarkdownRenderScheduler(maxConcurrent: 1, cacheLimit: 200)

  private let maxConcurrent: Int
  private let cacheLimit: Int

  private var running = 0
  private var waiters: [CheckedContinuation<Void, Never>] = []

  private var cache: [String: String] = [:]
  private var cacheKeys: [String] = []

  init(maxConcurrent: Int, cacheLimit: Int) {
    self.maxConcurrent = max(1, maxConcurrent)
    self.cacheLimit = max(1, cacheLimit)
  }

  func html(forKey key: String, render: @Sendable () -> String) async -> String {
    if let cached = cache[key] {
      return cached
    }

    await acquire()
    defer { release() }

    // Re-check after waiting since another request may have filled cache.
    if let cached = cache[key] {
      return cached
    }

    let html = render()
    store(html: html, forKey: key)
    return html
  }

  func clearCache() {
    cache.removeAll()
    cacheKeys.removeAll()
  }

  private func acquire() async {
    if running < maxConcurrent {
      running += 1
      return
    }
    await withCheckedContinuation { continuation in
      waiters.append(continuation)
    }
    running += 1
  }

  private func release() {
    running = max(0, running - 1)
    guard !waiters.isEmpty else { return }
    let next = waiters.removeFirst()
    next.resume()
  }

  private func store(html: String, forKey key: String) {
    cache[key] = html
    cacheKeys.removeAll { $0 == key }
    cacheKeys.append(key)

    while cacheKeys.count > cacheLimit {
      let stale = cacheKeys.removeFirst()
      cache.removeValue(forKey: stale)
    }
  }
}
