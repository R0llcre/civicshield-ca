import Foundation
import WebKit

/// Warms up WebKit so the Ask tab doesn't pay the WebContent process startup cost.
@MainActor
final class WebViewPrewarmer {
  static let shared = WebViewPrewarmer()

  private var didPrewarm = false
  private var webView: WKWebView?

  private init() {}

  func prewarm() {
    prewarmIfNeeded()
  }

  func prewarmIfNeeded() {
    guard !didPrewarm else { return }
    let interval = PerfSignpost.begin("webview_prewarm")
    defer { PerfSignpost.end("webview_prewarm", interval) }
    didPrewarm = true
    prewarmOnMain()
  }

  private func prewarmOnMain() {
    let wv = WKWebView(frame: .zero, configuration: WebViewShared.makeConfiguration())
    wv.isOpaque = false
    wv.backgroundColor = .clear
    wv.loadHTMLString("<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/></head><body></body></html>", baseURL: nil)
    webView = wv
  }
}
