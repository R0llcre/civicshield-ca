package org.regshield.ca.ui.screens.verify

import kotlin.math.roundToInt

enum class MatchTier {
  STRONG,
  LIKELY,
  POSSIBLE,
  LOW,
}

fun scoreToPercent(score: Double): Int {
  val raw =
    if (score <= 1.0) {
      score * 100.0
    } else {
      score
    }
  return raw.roundToInt().coerceIn(0, 100)
}

fun percentToTier(percent: Int): MatchTier {
  return when {
    percent >= 85 -> MatchTier.STRONG
    percent >= 65 -> MatchTier.LIKELY
    percent >= 40 -> MatchTier.POSSIBLE
    else -> MatchTier.LOW
  }
}

