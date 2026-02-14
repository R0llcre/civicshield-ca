import SwiftUI

@main
struct CivicShieldApp: App {
  @StateObject private var env = AppEnvironment()

  var body: some Scene {
    WindowGroup {
      AppRootView()
        .environmentObject(env)
    }
  }
}

