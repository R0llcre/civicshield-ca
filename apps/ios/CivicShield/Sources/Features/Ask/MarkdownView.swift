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

  @MainActor
  func makeUIView(context: Context) -> WKWebView {
    let config = WebViewShared.makeConfiguration()

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

  @MainActor
  func updateUIView(_ uiView: WKWebView, context: Context) {
    let trait = uiView.traitCollection
    let params = MarkdownRenderParams.from(trait: trait, accent: accent)
    context.coordinator.update(uiView: uiView, markdown: markdown, params: params)
  }

  final class Coordinator: NSObject, WKNavigationDelegate {
    private let height: Binding<CGFloat>
    private let onOpenURL: (URL) -> Void

    nonisolated private static let stripImgRegex: NSRegularExpression? = {
      // Remove <img ...> tags to avoid unintended external network fetches.
      try? NSRegularExpression(pattern: "<img\\b[^>]*>", options: [.caseInsensitive])
    }()

    nonisolated(unsafe) private static var didEmitFirstAssistantRenderBegin = false
    nonisolated(unsafe) private static var didEmitFirstAssistantRenderEnd = false

    private var lastKey: String = ""
    var lastHTML: String = ""
    private var inFlightKey: String? = nil
    private var renderToken: Int = 0
    private var renderTask: Task<Void, Never>?
    private var pendingHeightWorkItem: DispatchWorkItem?

    init(height: Binding<CGFloat>, onOpenURL: @escaping (URL) -> Void) {
      self.height = height
      self.onOpenURL = onOpenURL
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
      scheduleHeightUpdate(for: webView)
    }

    private func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
      if navigationAction.navigationType == .linkActivated, let url = navigationAction.request.url {
        if UrlPolicy.isAllowed(url) {
          onOpenURL(url)
        }
        decisionHandler(.cancel)
        return
      }
      decisionHandler(.allow)
    }

    func update(uiView: WKWebView, markdown: String, params: MarkdownRenderParams) {
      let key = cacheKey(markdown: markdown, params: params)

      // Fast path: already rendered for these inputs.
      if key == lastKey, !lastHTML.isEmpty {
        scheduleHeightUpdate(for: uiView)
        return
      }
      lastKey = key

      // Render already in progress for this key.
      if inFlightKey == key {
        return
      }
      inFlightKey = key

      if !Self.didEmitFirstAssistantRenderBegin {
        Self.didEmitFirstAssistantRenderBegin = true
        PerfSignpost.event("ask_first_assistant_markdown_begin")
      }

      renderToken &+= 1
      let token = renderToken
      renderTask?.cancel()
      renderTask = Task { [weak self, weak uiView] in
        guard let self, let uiView else { return }

        PerfSignpost.event("ask_markdown_render_begin")
        let html = await MarkdownRenderScheduler.shared.html(forKey: key) {
          Self.renderHTML(markdown: markdown, params: params)
        }
        guard !Task.isCancelled else { return }
        PerfSignpost.event("ask_markdown_render_end")

        if !Self.didEmitFirstAssistantRenderEnd {
          Self.didEmitFirstAssistantRenderEnd = true
          PerfSignpost.event("ask_first_assistant_markdown_end")
        }

        guard self.lastKey == key, self.renderToken == token else {
          if self.inFlightKey == key { self.inFlightKey = nil }
          return
        }

        self.inFlightKey = nil
        if self.lastHTML != html {
          self.lastHTML = html
          PerfSignpost.event("ask_webview_loadhtml_begin")
          uiView.loadHTMLString(html, baseURL: nil)
          PerfSignpost.event("ask_webview_loadhtml_end")
        } else {
          self.scheduleHeightUpdate(for: uiView)
        }
      }
    }

    func scheduleHeightUpdate(for webView: WKWebView) {
      pendingHeightWorkItem?.cancel()
      let work = DispatchWorkItem { [weak webView] in
        self.pendingHeightWorkItem = nil
        guard let webView else { return }
        let h = max(20, webView.scrollView.contentSize.height.rounded(.up))
        if abs(self.height.wrappedValue - h) > 1.0 {
          PerfSignpost.event("ask_markdown_height_writeback")
          self.height.wrappedValue = h
        }
      }
      pendingHeightWorkItem = work
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.12, execute: work)
    }

    private func cacheKey(markdown: String, params: MarkdownRenderParams) -> String {
      var hasher = Hasher()
      hasher.combine(markdown)
      hasher.combine(params.accentHex)
      hasher.combine(params.textHex)
      hasher.combine(params.secondaryHex)
      hasher.combine(params.borderHex)
      hasher.combine(params.uiStyle.rawValue)
      hasher.combine(Int(params.fontSize * 10))
      hasher.combine(Int(params.monoSize * 10))
      return String(hasher.finalize())
    }

    nonisolated static func renderHTML(markdown: String, params: MarkdownRenderParams) -> String {
      let bodyCSS = """
      :root { color-scheme: light dark; }
      html, body { margin: 0; padding: 0; background: transparent; }
      body {
        font-family: -apple-system, system-ui, "SF Pro Text", "SF Pro Display";
        font-size: \(params.fontSize)px;
        line-height: 1.35;
        color: \(params.textHex);
        word-wrap: break-word;
        /* Give list markers and first glyphs a tiny safety margin to avoid left-edge clipping. */
        padding: 0 2px;
      }
      p { margin: 0 0 10px 0; }
      a { color: \(params.accentHex); text-decoration: underline; }
      ul, ol { margin: 0 0 10px 0; padding-left: 22px; }
      li { margin: 4px 0; }
      blockquote {
        margin: 8px 0;
        padding: 8px 10px;
        border-left: 3px solid \(params.borderHex);
        color: \(params.secondaryHex);
        background: rgba(0,0,0,0.03);
        border-radius: 10px;
      }
      code {
        font-family: ui-monospace, Menlo, Monaco, SFMono-Regular, "SF Mono";
        font-size: \(params.monoSize)px;
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
        display: block;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
      }
      th, td {
        border: 1px solid \(params.borderHex);
        padding: 6px 8px;
        vertical-align: top;
      }
      th { background: rgba(0,0,0,0.04); text-align: left; }
      hr { border: none; border-top: 1px solid \(params.borderHex); margin: 12px 0; }
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

      var sanitized = htmlBody
        .replacingOccurrences(of: "<script", with: "&lt;script", options: [.caseInsensitive])
        .replacingOccurrences(of: "</script>", with: "&lt;/script&gt;", options: [.caseInsensitive])

      if let re = Self.stripImgRegex {
        let ns = sanitized as NSString
        sanitized = re.stringByReplacingMatches(in: sanitized, options: [], range: NSRange(location: 0, length: ns.length), withTemplate: "")
      }

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

  func toHex(resolvedWith trait: UITraitCollection) -> String? {
    self.resolvedColor(with: trait).toHex()
  }
}

private struct MarkdownRenderParams: Sendable {
  let fontSize: CGFloat
  let monoSize: CGFloat
  let accentHex: String
  let textHex: String
  let secondaryHex: String
  let borderHex: String
  let uiStyle: UIUserInterfaceStyle

  static func from(trait: UITraitCollection, accent: UIColor) -> MarkdownRenderParams {
    let fontSize = UIFont.preferredFont(forTextStyle: .body).pointSize
    let monoSize = UIFont.preferredFont(forTextStyle: .callout).pointSize

    let accentHex = accent.toHex(resolvedWith: trait) ?? "#0B4D91"
    let textHex = UIColor.label.toHex(resolvedWith: trait) ?? "#111827"
    let secondaryHex = UIColor.secondaryLabel.toHex(resolvedWith: trait) ?? "#475569"
    let borderHex = UIColor.separator.toHex(resolvedWith: trait) ?? "#CBD5E1"

    return MarkdownRenderParams(
      fontSize: fontSize,
      monoSize: monoSize,
      accentHex: accentHex,
      textHex: textHex,
      secondaryHex: secondaryHex,
      borderHex: borderHex,
      uiStyle: trait.userInterfaceStyle
    )
  }
}
