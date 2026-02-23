import SwiftUI
import UIKit

struct AppRootView: View {
  @EnvironmentObject private var env: AppEnvironment
  @State private var didStartup: Bool = false

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
    .task {
      guard !didStartup else { return }
      didStartup = true
      let startupInterval = PerfSignpost.begin("app_startup_task")
      defer { PerfSignpost.end("app_startup_task", startupInterval) }

      // Optional developer override: `--tab=verify|shield|ask|help`
      if let t = tabOverrideFromArgs() {
        env.tab = t
      }

      // Warm WebKit immediately so first Ask entry doesn't pay startup cost.
      PerfSignpost.event("webview_prewarm_begin")
      WebViewPrewarmer.shared.prewarmIfNeeded()
      PerfSignpost.event("webview_prewarm_end")
    }
  }

  private func tabOverrideFromArgs() -> AppTab? {
    // Keep this simple: a single launch argument is enough for CLI (simctl) runs.
    for a in ProcessInfo.processInfo.arguments {
      guard a.hasPrefix("--tab=") else { continue }
      let v = a.dropFirst("--tab=".count).lowercased()
      switch v {
      case "verify": return .verify
      case "shield": return .shield
      case "ask": return .ask
      case "help": return .help
      default: return nil
      }
    }
    return nil
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
    let shape = RoundedRectangle(cornerRadius: 18.5, style: .continuous)
    HStack(spacing: 8) {
      Circle()
        .fill(tab.accent)
        .frame(width: 6, height: 6)
      Text("CivicShield")
        .font(.system(size: 13, weight: .semibold, design: .rounded))
        .foregroundStyle(.primary)
        .lineLimit(1)
        .minimumScaleFactor(0.8)
    }
    .frame(width: 126, height: 37)
    .background(.ultraThinMaterial, in: shape)
    .overlay(
      shape.stroke(
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
    .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 8)
  }
}

extension View {
  func appChrome(tab: AppTab) -> some View {
    // Only nudge the main content slightly. TopPill itself should never overlap content.
    let pillTopPadding: CGFloat = 0
    let contentTopGap: CGFloat = 8
    // Visually tuck the pill closer to the Dynamic Island without using overlay (no content occlusion).
    // Tuck into the Dynamic Island area (it's ok if the island covers it).
    // Tuck the pill into the Dynamic Island area (it's ok if the island covers it).
    // NOTE: negative offset only affects visuals; layout height stays reserved (no content occlusion).
    let pillVisualOffsetY: CGFloat = -48
    return self
      .navigationBarHidden(true)
      .safeAreaInset(edge: .top, spacing: 0) {
        HStack {
          Spacer(minLength: 0)
          TopPill(tab: tab)
            .offset(y: pillVisualOffsetY)
          Spacer(minLength: 0)
        }
        .padding(.top, pillTopPadding)
        .padding(.bottom, contentTopGap)
        .frame(maxWidth: .infinity)
      }
  }
}
