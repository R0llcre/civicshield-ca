import SwiftUI

@main
struct CivicShieldApp: App {
  @StateObject private var env = AppEnvironment()
  @State private var didMarkFirstFrame = false

  var body: some Scene {
    WindowGroup {
      AppRootView()
        .environmentObject(env)
        .onAppear {
          guard !didMarkFirstFrame else { return }
          didMarkFirstFrame = true
          PerfSignpost.event("app_first_frame")
        }
    }
  }

  init() {
    PerfSignpost.event("app_launch_init")
  }
}
