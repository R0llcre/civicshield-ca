import SwiftUI

struct GlassCard<Content: View>: View {
  let content: Content

  init(@ViewBuilder content: () -> Content) {
    self.content = content()
  }

  var body: some View {
    VStack(alignment: .leading, spacing: 12) {
      content
    }
    .padding(16)
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
    .overlay(
      RoundedRectangle(cornerRadius: 24, style: .continuous)
        .stroke(
          LinearGradient(
            colors: [.white.opacity(0.9), .white.opacity(0.15)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
          ),
          lineWidth: 1
        )
    )
    .shadow(color: .black.opacity(0.08), radius: 18, x: 0, y: 12)
  }
}

struct PrimaryButtonStyle: ButtonStyle {
  let tint: Color

  func makeBody(configuration: Configuration) -> some View {
    configuration.label
      .font(.system(.subheadline, design: .rounded).weight(.semibold))
      .foregroundStyle(.white)
      .frame(maxWidth: .infinity, minHeight: 48)
      .background(tint.opacity(configuration.isPressed ? 0.85 : 1.0), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
      .shadow(color: tint.opacity(0.25), radius: 16, x: 0, y: 10)
      .scaleEffect(configuration.isPressed ? 0.99 : 1.0)
  }
}

struct TonalButtonStyle: ButtonStyle {
  let tint: Color

  func makeBody(configuration: Configuration) -> some View {
    configuration.label
      .font(.system(.subheadline, design: .rounded).weight(.semibold))
      .foregroundStyle(tint)
      .frame(maxWidth: .infinity, minHeight: 48)
      .background(tint.opacity(configuration.isPressed ? 0.16 : 0.12), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
      .overlay(
        RoundedRectangle(cornerRadius: 18, style: .continuous)
          .stroke(tint.opacity(0.20), lineWidth: 1)
      )
  }
}

struct StatusPill: View {
  let text: String
  let tint: Color

  var body: some View {
    Text(text)
      .font(.system(.caption, design: .rounded).weight(.semibold))
      .padding(.horizontal, 10)
      .padding(.vertical, 6)
      .background(tint.opacity(0.14), in: Capsule())
      .overlay(Capsule().stroke(tint.opacity(0.18), lineWidth: 1))
  }
}

