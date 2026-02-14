import Down
import SwiftUI
import UIKit

struct MarkdownView: UIViewRepresentable {
  let markdown: String
  let accent: UIColor
  let onOpenURL: (URL) -> Void

  func makeUIView(context: Context) -> UITextView {
    let tv = UITextView()
    tv.isEditable = false
    tv.isSelectable = true
    tv.isScrollEnabled = false
    tv.backgroundColor = .clear
    tv.textContainerInset = .zero
    tv.textContainer.lineFragmentPadding = 0
    tv.adjustsFontForContentSizeCategory = true
    tv.delegate = context.coordinator
    tv.linkTextAttributes = [
      .foregroundColor: accent,
      .underlineStyle: NSUnderlineStyle.single.rawValue,
    ]
    return tv
  }

  func updateUIView(_ uiView: UITextView, context: Context) {
    uiView.attributedText = render(markdown: markdown, accent: accent)
  }

  func makeCoordinator() -> Coordinator {
    Coordinator(onOpenURL: onOpenURL)
  }

  final class Coordinator: NSObject, UITextViewDelegate {
    let onOpenURL: (URL) -> Void

    init(onOpenURL: @escaping (URL) -> Void) {
      self.onOpenURL = onOpenURL
    }

    func textView(_ textView: UITextView, shouldInteractWith URL: URL, in characterRange: NSRange, interaction: UITextItemInteraction) -> Bool {
      if UrlPolicy.isAllowed(URL) {
        onOpenURL(URL)
      }
      // Block non-allowlisted links.
      return false
    }
  }

  private func render(markdown: String, accent: UIColor) -> NSAttributedString {
    // Down supports GitHub-flavored markdown (tables, strikethrough, task lists, etc.).
    // We keep styling minimal here to avoid breaking semantic formatting.
    do {
      let down = Down(markdownString: markdown)
      return try down.toAttributedString()
    } catch {
      return NSAttributedString(string: markdown)
    }
  }
}

