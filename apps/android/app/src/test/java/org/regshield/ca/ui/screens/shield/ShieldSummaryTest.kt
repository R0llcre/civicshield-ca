package org.regshield.ca.ui.screens.shield

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShieldSummaryTest {
  @Test
  fun buildShieldSummary_includesSelectionsAndDisclaimer() {
    val summary =
      buildShieldSummary(
        appName = "CivicShield",
        selectedFlags = listOf("Asks for payment by gift card or crypto"),
        steps = listOf(
          "Verify identities and licenses" to true,
          "Request written agreements" to false,
        ),
        disclaimer = "Information only. This is not legal advice.",
      )

    assertThat(summary).contains("Red flags selected (1):")
    assertThat(summary).contains("gift card or crypto")
    assertThat(summary).contains("Action plan (1/2 done):")
    assertThat(summary).contains("[x] Verify identities and licenses")
    assertThat(summary).contains("[ ] Request written agreements")
    assertThat(summary).contains("Information only. This is not legal advice.")
  }
}
