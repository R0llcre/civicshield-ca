import Foundation

enum ApiError: Error, LocalizedError {
  case invalidBaseURL
  case invalidResponse
  case http(status: Int, message: String?)
  case decoding(message: String)

  var errorDescription: String? {
    switch self {
    case .invalidBaseURL:
      return "API base URL is invalid."
    case .invalidResponse:
      return "Invalid server response."
    case let .http(status, message):
      if let message, !message.isEmpty { return message }
      return "HTTP \(status)"
    case let .decoding(message):
      return "Failed to decode response: \(message)"
    }
  }
}

// URLSession is thread-safe; ApiClient is immutable after init.
final class ApiClient: @unchecked Sendable {
  let baseURL: URL
  private let session: URLSession

  init(baseURL: URL, session: URLSession = .shared) {
    self.baseURL = baseURL
    self.session = session
  }

  func get<T: Decodable>(_ path: String, query: [URLQueryItem] = [], timeout: TimeInterval = 30) async throws -> T {
    guard var url = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else {
      throw ApiError.invalidBaseURL
    }
    url.path = normalizedPath(path)
    if !query.isEmpty {
      url.queryItems = query
    }
    guard let finalURL = url.url else { throw ApiError.invalidBaseURL }
    var req = URLRequest(url: finalURL)
    req.httpMethod = "GET"
    req.timeoutInterval = timeout
    return try await run(req)
  }

  func post<T: Decodable, Body: Encodable>(_ path: String, body: Body, timeout: TimeInterval = 30) async throws -> T {
    guard let url = URL(string: normalizedPath(path), relativeTo: baseURL) else { throw ApiError.invalidBaseURL }
    var req = URLRequest(url: url)
    req.httpMethod = "POST"
    req.timeoutInterval = timeout
    req.setValue("application/json", forHTTPHeaderField: "Content-Type")
    req.httpBody = try JSONEncoder().encode(body)
    return try await run(req)
  }

  private func run<T: Decodable>(_ req: URLRequest) async throws -> T {
    let (data, resp) = try await session.data(for: req)
    guard let http = resp as? HTTPURLResponse else { throw ApiError.invalidResponse }
    if !(200...299).contains(http.statusCode) {
      let msg = (try? JSONDecoder().decode(ApiErrorBody.self, from: data))?.error
      throw ApiError.http(status: http.statusCode, message: msg)
    }
    do {
      return try JSONDecoder().decode(T.self, from: data)
    } catch {
      throw ApiError.decoding(message: error.localizedDescription)
    }
  }

  private func normalizedPath(_ path: String) -> String {
    if path.hasPrefix("/") { return path }
    return "/" + path
  }
}

private struct ApiErrorBody: Decodable {
  let error: String?
}
