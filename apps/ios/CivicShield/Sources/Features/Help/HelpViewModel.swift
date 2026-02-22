import Foundation

@MainActor
final class HelpViewModel: ObservableObject {
  @Published var county: String = ""
  @Published var language: String = ""
  @Published var service: String = ""
  @Published var loading: Bool = false
  @Published var error: String? = nil
  @Published var results: [AidOrg] = []
  @Published var hasSearched: Bool = false

  private var didInitialLoad = false

  func initialLoadIfNeeded(api: ApiClient) async {
    guard !didInitialLoad else { return }
    didInitialLoad = true
    await search(api: api, featured: true)
  }

  func search(api: ApiClient, featured: Bool = false) async {
    loading = true
    error = nil
    do {
      var items: [URLQueryItem] = []
      if !featured {
        if !county.isEmpty { items.append(URLQueryItem(name: "county", value: county)) }
        if !language.isEmpty { items.append(URLQueryItem(name: "lang", value: language)) }
        if !service.isEmpty { items.append(URLQueryItem(name: "service", value: service)) }
      }
      let res: DirectoryResponse = try await api.get("/api/directory/search", query: items, timeout: 30)
      let sorted = res.results.sorted { a, b in
        let rank: (String) -> Int = { s in
          switch s.lowercased() {
          case "open": return 0
          case "waitlist": return 1
          case "closed": return 2
          default: return 9
          }
        }
        let ra = rank(a.intakeStatus)
        let rb = rank(b.intakeStatus)
        if ra != rb { return ra < rb }
        return a.name < b.name
      }
      self.results = sorted
      self.hasSearched = true
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "Request failed"
      self.hasSearched = true
    }
    loading = false
  }
}

