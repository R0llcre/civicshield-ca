package org.regshield.ca.ui.screens.shield

fun buildShieldSummary(
  appName: String,
  selectedFlags: List<String>,
  steps: List<Pair<String, Boolean>>,
  disclaimer: String,
): String {
  val sb = StringBuilder()
  sb.appendLine("$appName - Scam shield summary")
  sb.appendLine()

  sb.appendLine("Red flags selected (${selectedFlags.size}):")
  if (selectedFlags.isEmpty()) {
    sb.appendLine("- None")
  } else {
    for (t in selectedFlags) {
      sb.appendLine("- $t")
    }
  }
  sb.appendLine()

  val doneCount = steps.count { it.second }
  sb.appendLine("Action plan ($doneCount/${steps.size} done):")
  for ((text, done) in steps) {
    sb.appendLine("${if (done) "[x]" else "[ ]"} $text")
  }
  sb.appendLine()

  sb.appendLine(disclaimer)
  return sb.toString().trimEnd()
}
