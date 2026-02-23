import Foundation

struct ShieldFlag: Identifiable, Hashable {
  let id: String
  let title: String
  let detail: String
}

struct ShieldStep: Identifiable, Hashable {
  let id: String
  let text: String
}

@MainActor
final class ShieldViewModel: ObservableObject {
  @Published private(set) var selectedFlags: Set<String>
  @Published private(set) var completedSteps: Set<String>

  private let flagsKey = "shield.selectedFlags.v1"
  private let stepsKey = "shield.completedSteps.v1"

  init() {
    let f = Set(UserDefaults.standard.stringArray(forKey: flagsKey) ?? [])
    let s = Set(UserDefaults.standard.stringArray(forKey: stepsKey) ?? [])
    self.selectedFlags = f
    self.completedSteps = s
  }

  let flags: [ShieldFlag] = [
    ShieldFlag(id: "flag_1", title: "Asks for payment by gift card or crypto", detail: "Legitimate legal services rarely require these payment methods."),
    ShieldFlag(id: "flag_2", title: "Pressures you to act immediately", detail: "Scams create urgency to prevent verification."),
    ShieldFlag(id: "flag_3", title: "Refuses to share license or registration details", detail: "Legitimate providers should be verifiable via official directories."),
    ShieldFlag(id: "flag_4", title: "Requests sensitive originals before trust is established", detail: "Keep originals and share copies only after verification."),
    ShieldFlag(id: "flag_5", title: "Only communicates off-platform", detail: "Prefer official channels and keep written records."),
  ]

  let steps: [ShieldStep] = [
    ShieldStep(id: "step_verify", text: "Verify identities and licenses using official registries."),
    ShieldStep(id: "step_receipts", text: "Request written agreements and itemized receipts."),
    ShieldStep(id: "step_no_blank", text: "Never sign blank forms or share originals without verification."),
  ]

  func toggleFlag(_ id: String) {
    if selectedFlags.contains(id) {
      selectedFlags.remove(id)
    } else {
      selectedFlags.insert(id)
    }
    persist()
  }

  func toggleStep(_ id: String) {
    if completedSteps.contains(id) {
      completedSteps.remove(id)
    } else {
      completedSteps.insert(id)
    }
    persist()
  }

  func clearAllFlags() {
    selectedFlags.removeAll()
    persist()
  }

  func riskLabel() -> String {
    switch selectedFlags.count {
    case 0...1: return "Overall risk: Low"
    case 2...3: return "Overall risk: Medium"
    default: return "Overall risk: High"
    }
  }

  func planProgressLabel() -> String {
    "Steps: \(completedSteps.count)/\(steps.count) done"
  }

  func summaryText() -> String {
    var lines: [String] = []
    lines.append("CivicShield summary (information only, not legal advice)")
    lines.append("")
    lines.append("Red flags (\(selectedFlags.count)/\(flags.count)):")
    if selectedFlags.isEmpty {
      lines.append("- None selected")
    } else {
      for f in flags where selectedFlags.contains(f.id) {
        lines.append("- \(f.title)")
      }
    }
    lines.append("")
    lines.append("Action plan (\(completedSteps.count)/\(steps.count)):")
    for (i, s) in steps.enumerated() {
      let mark = completedSteps.contains(s.id) ? "x" : " "
      lines.append("\(i + 1). [\(mark)] \(s.text)")
    }
    lines.append("")
    lines.append("Tip: verify using official sources and contact legal aid if unsure.")
    return lines.joined(separator: "\n")
  }

  private func persist() {
    UserDefaults.standard.set(Array(selectedFlags), forKey: flagsKey)
    UserDefaults.standard.set(Array(completedSteps), forKey: stepsKey)
  }
}
