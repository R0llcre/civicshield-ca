import WebKit

@MainActor
enum WebViewShared {
  private static let sharedProcessPool = WKProcessPool()
  private static let sharedDataStore = WKWebsiteDataStore.nonPersistent()

  static func makeConfiguration() -> WKWebViewConfiguration {
    let config = WKWebViewConfiguration()
    config.websiteDataStore = sharedDataStore
    config.processPool = sharedProcessPool
    if #available(iOS 14.0, *) {
      config.defaultWebpagePreferences.allowsContentJavaScript = false
    } else {
      config.preferences.javaScriptEnabled = false
    }
    return config
  }
}
