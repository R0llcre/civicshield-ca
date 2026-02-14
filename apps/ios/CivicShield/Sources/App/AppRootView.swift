import SwiftUI

struct AppRootView: View {
  @EnvironmentObject private var env: AppEnvironment

  var body: some View {
    ZStack {
      CivicBackground()
        .ignoresSafeArea()

      TabView(selection: $env.tab) {
        NavigationStack {
          VerifyView()
            .appChrome(tab: .verify)
        }
        .tag(AppTab.verify)
        .tabItem { Label(AppTab.verify.title, systemImage: AppTab.verify.systemImage) }

        NavigationStack {
          ShieldView()
            .appChrome(tab: .shield)
        }
        .tag(AppTab.shield)
        .tabItem { Label(AppTab.shield.title, systemImage: AppTab.shield.systemImage) }

        NavigationStack {
          AskView()
            .appChrome(tab: .ask)
        }
        .tag(AppTab.ask)
        .tabItem { Label(AppTab.ask.title, systemImage: AppTab.ask.systemImage) }

        NavigationStack {
          HelpView()
            .appChrome(tab: .help)
        }
        .tag(AppTab.help)
        .tabItem { Label(AppTab.help.title, systemImage: AppTab.help.systemImage) }
      }
    }
  }
}

private struct CivicBackground: View {
  var body: some View {
    ZStack {
      LinearGradient(
        colors: [
          Color(red: 245/255, green: 250/255, blue: 255/255),
          Color(red: 239/255, green: 247/255, blue: 252/255),
          Color(red: 250/255, green: 250/255, blue: 250/255),
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
      )

      // Soft glow.
      RadialGradient(
        colors: [Color(red: 11/255, green: 77/255, blue: 145/255).opacity(0.12), .clear],
        center: .topTrailing,
        startRadius: 20,
        endRadius: 420
      )
      RadialGradient(
        colors: [Color(red: 14/255, green: 138/255, blue: 106/255).opacity(0.08), .clear],
        center: .bottomLeading,
        startRadius: 20,
        endRadius: 520
      )
    }
  }
}

private struct TopPill: View {
  let tab: AppTab

  var body: some View {
    HStack(spacing: 8) {
      Circle()
        .fill(tab.accent)
        .frame(width: 8, height: 8)
      Text("CivicShield")
        .font(.system(.subheadline, design: .rounded).weight(.semibold))
        .foregroundStyle(.primary)
    }
    .padding(.horizontal, 14)
    .padding(.vertical, 8)
    .background(.ultraThinMaterial, in: Capsule())
    .overlay(
      Capsule()
        .stroke(
          LinearGradient(
            colors: [
              .white.opacity(0.95),
              tab.accent.opacity(0.25),
              .white.opacity(0.55),
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
          ),
          lineWidth: 1
        )
    )
    .shadow(color: .black.opacity(0.10), radius: 16, x: 0, y: 10)
    .padding(.top, 6)
    .padding(.bottom, 10)
  }
}

extension View {
  func appChrome(tab: AppTab) -> some View {
    self
      .safeAreaInset(edge: .top, spacing: 0) {
        TopPill(tab: tab)
      }
      .navigationBarHidden(true)
  }
}
