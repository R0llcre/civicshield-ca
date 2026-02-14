import SwiftUI

enum AppTab: Hashable {
  case verify
  case shield
  case ask
  case help

  var title: String {
    switch self {
    case .verify: return "Verify"
    case .shield: return "Shield"
    case .ask: return "Ask"
    case .help: return "Help"
    }
  }

  var systemImage: String {
    switch self {
    case .verify: return "checkmark.seal"
    case .shield: return "shield"
    case .ask: return "bubble.left.and.bubble.right"
    case .help: return "lifepreserver"
    }
  }

  var accent: Color {
    switch self {
    case .verify: return Color(red: 11/255, green: 77/255, blue: 145/255) // civic blue
    case .shield: return Color(red: 14/255, green: 138/255, blue: 106/255) // civic teal
    case .ask: return Color(red: 217/255, green: 119/255, blue: 6/255) // amber
    case .help: return Color(red: 2/255, green: 132/255, blue: 199/255) // sky
    }
  }
}

