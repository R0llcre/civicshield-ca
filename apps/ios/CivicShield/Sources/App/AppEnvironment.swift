import Foundation

@MainActor
final class AppEnvironment: ObservableObject {
  let api: ApiClient
  @Published var tab: AppTab = .verify

  init() {
    self.api = ApiClient(baseURL: AppConfig.apiBaseURL)

    // Developer helper: allow launching directly into a tab via `simctl launch ... --args --tab=ask`.
    // This is only for local debugging (e.g. screenshots/perf profiling).
    if let t = Self.tabOverrideFromArgs() {
      self.tab = t
    }
  }

  private static func tabOverrideFromArgs() -> AppTab? {
    for a in ProcessInfo.processInfo.arguments {
      guard a.hasPrefix("--tab=") else { continue }
      let v = a.dropFirst("--tab=".count).lowercased()
      switch v {
      case "verify": return .verify
      case "shield": return .shield
      case "ask": return .ask
      case "help": return .help
      default: return nil
      }
    }
    return nil
  }
}

enum AppConfig {
  static var apiBaseURL: URL {
    let raw = Bundle.main.object(forInfoDictionaryKey: "REGSHIELD_API_BASE_URL") as? String ?? ""
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if let url = URL(string: trimmed), url.scheme != nil {
      return url
    }
    // Safe fallback (will fail fast in networking).
    return URL(string: "https://invalid.local")!
  }
}
