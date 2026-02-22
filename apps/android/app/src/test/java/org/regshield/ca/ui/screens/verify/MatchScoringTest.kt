package org.regshield.ca.ui.screens.verify

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchScoringTest {
  @Test
  fun scoreToPercent_handlesSimilarityAndPercentScores() {
    assertThat(scoreToPercent(0.0)).isEqualTo(0)
    assertThat(scoreToPercent(0.63)).isEqualTo(63)
    assertThat(scoreToPercent(1.0)).isEqualTo(100)
    assertThat(scoreToPercent(92.0)).isEqualTo(92)
    assertThat(scoreToPercent(150.0)).isEqualTo(100)
    assertThat(scoreToPercent(-10.0)).isEqualTo(0)
  }

  @Test
  fun percentToTier_thresholds() {
    assertThat(percentToTier(99)).isEqualTo(MatchTier.STRONG)
    assertThat(percentToTier(85)).isEqualTo(MatchTier.STRONG)
    assertThat(percentToTier(84)).isEqualTo(MatchTier.LIKELY)
    assertThat(percentToTier(65)).isEqualTo(MatchTier.LIKELY)
    assertThat(percentToTier(64)).isEqualTo(MatchTier.POSSIBLE)
    assertThat(percentToTier(40)).isEqualTo(MatchTier.POSSIBLE)
    assertThat(percentToTier(39)).isEqualTo(MatchTier.LOW)
    assertThat(percentToTier(0)).isEqualTo(MatchTier.LOW)
  }
}

