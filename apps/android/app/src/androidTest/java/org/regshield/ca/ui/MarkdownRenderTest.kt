package org.regshield.ca.ui

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.regshield.ca.ui.components.CivicCard
import org.regshield.ca.ui.components.MarkdownText
import org.regshield.ca.ui.theme.RegShieldTheme

class MarkdownRenderTest {
  @get:Rule
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun markdown_rendersWithoutRawMarkers() {
    val md =
      """
      To verify an accredited representative in California:

      1. **Check the Official Roster**
         - Go to the [EOIR roster](https://www.justice.gov/eoir/recognition-and-accreditation-roster-reports).
         - Confirm both the person and organization appear.

      Inline code: `EOIR`

      | A | B |
      | - | - |
      | 1 | 2 |
      """.trimIndent()

    composeRule.setContent {
      RegShieldTheme {
        CivicCard(modifier = Modifier.fillMaxWidth()) {
          MarkdownText(
            markdown = md,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            textColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            linkColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            onOpenUrl = {},
            viewTag = "markdown_test",
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    composeRule.waitForIdle()
    val tv = composeRule.activity.window.decorView.findViewWithTag<TextView>("markdown_test")
    val rendered = tv.text.toString()

    // Visible content exists.
    assertTrue(rendered.contains("Check the Official Roster"))
    assertTrue(rendered.contains("EOIR roster"))

    // Raw markdown markers should not be present in rendered text.
    assertFalse(rendered.contains("**"))
    assertFalse(rendered.contains("]("))
  }
}
