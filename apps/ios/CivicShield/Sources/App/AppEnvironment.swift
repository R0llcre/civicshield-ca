import Foundation

@MainActor
final class AppEnvironment: ObservableObject {
  let api: ApiClient
  @Published var tab: AppTab = .verify

  init() {
    self.api = ApiClient(baseURL: AppConfig.apiBaseURL)
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
