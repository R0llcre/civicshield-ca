import Foundation

@MainActor
final class VerifyViewModel: ObservableObject {
  @Published var name: String = ""
  @Published var state: String = "CA"
  @Published var loading: Bool = false
  @Published var error: String? = nil
  @Published var results: [RaEntry] = []
  @Published var freshness: DataFreshness? = nil

  private var didSearch: Bool = false

  func search(api: ApiClient) async {
    let q = name.trimmingCharacters(in: .whitespacesAndNewlines)
    let st = state.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    guard !q.isEmpty else {
      error = "Please enter a name."
      return
    }

    loading = true
    error = nil
    didSearch = true
    do {
      let res: VerifyResponse = try await api.post("/api/verify/ra", body: VerifyRequest(name: q, state: st), timeout: 30)
      self.results = res.results
      self.freshness = res.dataFreshness
    } catch {
      self.error = (error as? LocalizedError)?.errorDescription ?? "Request failed"
    }
    loading = false
  }

  var shouldShowNoResults: Bool {
    didSearch && !loading && error == nil && results.isEmpty
  }
}

