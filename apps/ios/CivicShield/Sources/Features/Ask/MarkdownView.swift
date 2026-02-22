import Down
import SwiftUI
import UIKit
import WebKit

/// Renders full GitHub-flavored markdown without relying on NSAttributedString(html:),
/// which has been observed to crash on some iOS versions/devices.
///
/// Implementation notes:
/// - Markdown -> HTML via Down (cmark-gfm).
/// - HTML rendered in a non-persistent WKWebView with JavaScript disabled.
/// - External images are stripped to avoid unintended network fetches.
/// - Link taps are intercepted and allowlisted via UrlPolicy.
struct MarkdownView: View {
  let markdown: String
  let accent: UIColor
  let onOpenURL: (URL) -> Void

  @State private var contentHeight: CGFloat = 20

  var body: some View {
    MarkdownWebView(
      markdown: markdown,
      accent: accent,
      onOpenURL: onOpenURL,
      height: $contentHeight
    )
    .frame(maxWidth: .infinity)
    .frame(height: max(20, contentHeight))
  }
}

private struct MarkdownWebView: UIViewRepresentable {
  let markdown: String
  let accent: UIColor
  let onOpenURL: (URL) -> Void
  @Binding var height: CGFloat

  func makeCoordinator() -> Coordinator {
    Coordinator(height: $height, onOpenURL: onOpenURL)
  }

  func makeUIView(context: Context) -> WKWebView {
    let config = WKWebViewConfiguration()
    config.websiteDataStore = .nonPersistent()
    if #available(iOS 14.0, *) {
      config.defaultWebpagePreferences.allowsContentJavaScript = false
    } else {
      config.preferences.javaScriptEnabled = false
    }

    let webView = WKWebView(frame: .zero, configuration: config)
    webView.navigationDelegate = context.coordinator
    webView.isOpaque = false
    webView.backgroundColor = .clear
    webView.scrollView.isScrollEnabled = false
    webView.scrollView.bounces = false
    webView.scrollView.backgroundColor = .clear
    webView.scrollView.showsVerticalScrollIndicator = false
    webView.scrollView.showsHorizontalScrollIndicator = false
    webView.scrollView.contentInset = .zero
    return webView
  }

  func updateUIView(_ uiView: WKWebView, context: Context) {
    let html = context.coordinator.renderHTML(markdown: markdown, accent: accent)
    if context.coordinator.lastHTML != html {
      context.coordinator.lastHTML = html
      uiView.loadHTMLString(html, baseURL: nil)
      return
    }

    // Re-measure in case width/trait changes affect layout.
    context.coordinator.scheduleHeightUpdate(for: uiView)
  }

  final class Coordinator: NSObject, WKNavigationDelegate {
    private let height: Binding<CGFloat>
    private let onOpenURL: (URL) -> Void

    var lastHTML: String = ""

    init(height: Binding<CGFloat>, onOpenURL: @escaping (URL) -> Void) {
      self.height = height
      self.onOpenURL = onOpenURL
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
      scheduleHeightUpdate(for: webView)
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
      if navigationAction.navigationType == .linkActivated, let url = navigationAction.request.url {
        if UrlPolicy.isAllowed(url) {
          onOpenURL(url)
        }
        decisionHandler(.cancel)
        return
      }
      decisionHandler(.allow)
    }

    func scheduleHeightUpdate(for webView: WKWebView) {
      // Avoid synchronous state updates during view update/layout.
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak webView] in
        guard let webView else { return }
        let h = max(20, webView.scrollView.contentSize.height.rounded(.up))
        if abs(self.height.wrappedValue - h) > 0.5 {
          self.height.wrappedValue = h
        }
      }
    }

    func renderHTML(markdown: String, accent: UIColor) -> String {
      let fontSize = UIFont.preferredFont(forTextStyle: .body).pointSize
      let monoSize = UIFont.preferredFont(forTextStyle: .callout).pointSize
      let textColor = UIColor.label
      let secondary = UIColor.secondaryLabel
      let border = UIColor.separator

      let accentHex = accent.toHex() ?? "#0B4D91"
      let textHex = textColor.toHex() ?? "#111827"
      let secondaryHex = secondary.toHex() ?? "#475569"
      let borderHex = border.toHex() ?? "#CBD5E1"

      let bodyCSS = """
      :root { color-scheme: light dark; }
      html, body { margin: 0; padding: 0; background: transparent; }
      body {
        font-family: -apple-system, system-ui, "SF Pro Text", "SF Pro Display";
        font-size: \(fontSize)px;
        line-height: 1.35;
        color: \(textHex);
        word-wrap: break-word;
      }
      p { margin: 0 0 10px 0; }
      a { color: \(accentHex); text-decoration: underline; }
      ul, ol { margin: 0 0 10px 18px; padding: 0; }
      li { margin: 4px 0; }
      blockquote {
        margin: 8px 0;
        padding: 8px 10px;
        border-left: 3px solid \(borderHex);
        color: \(secondaryHex);
        background: rgba(0,0,0,0.03);
        border-radius: 10px;
      }
      code {
        font-family: ui-monospace, Menlo, Monaco, SFMono-Regular, "SF Mono";
        font-size: \(monoSize)px;
        background: rgba(0,0,0,0.05);
        padding: 2px 4px;
        border-radius: 6px;
      }
      pre {
        overflow-x: auto;
        background: rgba(0,0,0,0.05);
        padding: 10px 12px;
        border-radius: 12px;
        border: 1px solid rgba(0,0,0,0.06);
      }
      pre code { background: transparent; padding: 0; }
      table {
        border-collapse: collapse;
        width: 100%;
        margin: 10px 0;
      }
      th, td {
        border: 1px solid \(borderHex);
        padding: 6px 8px;
        vertical-align: top;
      }
      th { background: rgba(0,0,0,0.04); text-align: left; }
      hr { border: none; border-top: 1px solid \(borderHex); margin: 12px 0; }
      /* Do not load external images. */
      img { display: none !important; }
      """

      let htmlBody: String
      do {
        let down = Down(markdownString: markdown)
        htmlBody = try down.toHTML()
      } catch {
        // As a fallback, escape and show plain text.
        htmlBody = "<pre>\(markdown.htmlEscaped())</pre>"
      }

      let sanitized = htmlBody
        .replacingOccurrences(of: "<script", with: "&lt;script", options: [.caseInsensitive])
        .replacingOccurrences(of: "</script>", with: "&lt;/script&gt;", options: [.caseInsensitive])

      return """
      <!doctype html>
      <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
          <style>\(bodyCSS)</style>
        </head>
        <body>\(sanitized)</body>
      </html>
      """
    }
  }
}

private extension String {
  func htmlEscaped() -> String {
    self
      .replacingOccurrences(of: "&", with: "&amp;")
      .replacingOccurrences(of: "<", with: "&lt;")
      .replacingOccurrences(of: ">", with: "&gt;")
      .replacingOccurrences(of: "\"", with: "&quot;")
      .replacingOccurrences(of: "'", with: "&#39;")
  }
}

private extension UIColor {
  func toHex() -> String? {
    var r: CGFloat = 0
    var g: CGFloat = 0
    var b: CGFloat = 0
    var a: CGFloat = 0
    guard self.getRed(&r, green: &g, blue: &b, alpha: &a) else { return nil }
    return String(format: "#%02X%02X%02X", Int(r * 255.0), Int(g * 255.0), Int(b * 255.0))
  }
}

