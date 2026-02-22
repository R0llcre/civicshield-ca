import Foundation

enum UrlPolicy {
  // Keep consistent with server allowlist (Ask citations) and Android client.
  static let allowedDomains: [String] = [
    "uscis.gov",
    "justice.gov",
    "eoir.justice.gov",
    "ca.gov",
    "calbar.ca.gov",
    "sos.ca.gov",
    "lawhelpca.org",
    "immigrationadvocates.org",
  ]

  static func isAllowed(_ url: URL) -> Bool {
    guard let scheme = url.scheme?.lowercased(), scheme == "https" || scheme == "http" else {
      return false
    }
    let host = (url.host ?? "").lowercased()
    if host.isEmpty { return false }
    return allowedDomains.contains(where: { host == $0 || host.hasSuffix("." + $0) })
  }
}

