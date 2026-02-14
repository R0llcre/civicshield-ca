package org.regshield.ca.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.regshield.ca.api.AidOrg
import org.regshield.ca.api.RaEntry
import org.regshield.ca.ui.screens.help.HelpContent
import org.regshield.ca.ui.screens.help.HelpFilters
import org.regshield.ca.ui.screens.help.HelpUiState
import org.regshield.ca.ui.screens.ask.AskContent
import org.regshield.ca.ui.screens.shield.ShieldContent
import org.regshield.ca.ui.screens.verify.VerifyContent
import org.regshield.ca.ui.theme.RegShieldTheme
import org.regshield.ca.vault.VaultItemMeta
import org.regshield.ca.vault.VaultUiState
import org.regshield.ca.vault.VaultContent

class AppUiFlowsTest {
  @get:Rule
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun verifyScreen_rendersAndSearchActionWorks() {
    var searched = false
    composeRule.setContent {
      RegShieldTheme {
        VerifyContent(
          isCompact = true,
          name = "Diaz",
          stateCode = "CA",
          hasSearched = true,
          queryName = "Diaz",
          queryState = "CA",
          loading = false,
          errorResId = null,
          errorMessage = null,
          dataFreshnessEndedAt = "2026-02-13T06:00:00Z",
          results = listOf(
            RaEntry(
              name = "Diaz, Samantha",
              entryType = "representative",
              sourceUrl = "https://example.org/roster",
              score = 0.92,
              matchedOn = "name",
            ),
          ),
          onNameChange = {},
          onStateChange = {},
          onSearch = { searched = true },
          onOpenUrl = {},
          onCallPhone = {},
          onOpenCalBar = {},
          onOpenSos = {},
        )
      }
    }

    composeRule.onNodeWithTag("verify_name_input").assertExists()
    composeRule.onNodeWithTag("verify_search_button").performClick()
    composeRule.onNodeWithTag("verify_result_item_0").assertExists()
    composeRule.onNodeWithTag("verify_scroll").performScrollToNode(hasTestTag("verify_result_item_0"))
    composeRule.onNodeWithTag("verify_match_chip_0", useUnmergedTree = true).assertExists()
    assertTrue(searched)
  }

  @Test
  fun helpScreen_rendersFiltersAndStatusChip() {
    composeRule.setContent {
      RegShieldTheme {
        HelpContent(
          isCompact = true,
          state = HelpUiState(
            filters = HelpFilters(county = "Los Angeles", language = "Spanish", service = "Asylum"),
            results = listOf(
              AidOrg(
                id = "1",
                name = "Sample Legal Aid",
                intakeStatus = "open",
                counties = listOf("Los Angeles"),
                languages = listOf("English", "Spanish"),
                services = listOf("Asylum"),
                sourceUrl = "https://example.org/source",
              ),
            ),
          ),
          onCounty = {},
          onLanguage = {},
          onService = {},
          onSearch = {},
          onOpenOrgLink = {},
          onOpenUrl = {},
        )
      }
    }

    composeRule.onNodeWithTag("help_county_filter").assertExists()
    composeRule.onNodeWithTag("help_search_button").assertExists()
    composeRule.onNodeWithTag("help_result_item_0").assertExists()
    composeRule.onNodeWithTag("help_scroll").performScrollToNode(hasTestTag("help_result_item_0"))
    composeRule.onNodeWithTag("help_status_chip", useUnmergedTree = true).assertExists()
  }

  @Test
  fun shieldScreen_checkboxRowAndVaultActionExist() {
    composeRule.setContent {
      RegShieldTheme {
        ShieldContent(
          isCompact = true,
          selectedFlagIds = setOf("pay"),
          completedStepIds = setOf("verify"),
          onToggleFlag = { _, _ -> },
          onToggleStep = { _, _ -> },
          onClearAll = {},
          onCopySummary = {},
          onShareSummary = {},
          onOpenVault = {},
          onGoVerify = {},
          onGoHelp = {},
        )
      }
    }

    composeRule.onNodeWithTag("shield_flag_0").assertExists()
    composeRule.onNodeWithTag("shield_scroll").performScrollToNode(hasTestTag("shield_vault_card"))
    composeRule.onNodeWithTag("shield_open_vault", useUnmergedTree = true).assertExists()
    composeRule.onNodeWithTag("shield_copy_summary").assertExists()
    composeRule.onNodeWithTag("shield_share_summary").assertExists()
    composeRule.onNodeWithTag("shield_clear_all").assertExists()
    composeRule.onNodeWithTag("shield_step_0").assertExists()
  }

  @Test
  fun vaultScreen_lockedStateActionsExist() {
    composeRule.setContent {
      RegShieldTheme {
        VaultContent(
          isCompact = true,
          state = VaultUiState(
            hasVault = true,
            locked = true,
            busy = false,
            error = null,
            items = listOf(
              VaultItemMeta(
                id = "a",
                createdAt = "2026-02-13T00:00:00Z",
                originalName = "note.txt",
                contentType = "text/plain",
                size = 120L,
              ),
            ),
          ),
          passphrase = "secret",
          onPassphraseChange = {},
          onClose = {},
          onUnlock = {},
          onCreate = {},
          onImport = {},
          onAddFiles = {},
          onExport = {},
          onLock = {},
          onDelete = {},
        )
      }
    }

    composeRule.onNodeWithTag("vault_passphrase_input").assertExists()
    composeRule.onNodeWithTag("vault_unlock_button").assertExists()
  }

  @Test
  fun askScreen_rendersInputAndSendAndClear() {
    composeRule.setContent {
      RegShieldTheme {
        AskContent(
          isCompact = true,
          consentAccepted = false,
          messages = emptyList(),
          sending = false,
          error = null,
          draft = "What is an accredited representative?",
          onDraftChange = {},
          onClearChat = {},
          onSend = {},
          onAction = {},
          onOpenCitation = {},
        )
      }
    }

    composeRule.onNodeWithTag("ask_input").assertExists()
    composeRule.onNodeWithTag("ask_send").assertExists()
    composeRule.onNodeWithTag("ask_clear_chat").assertExists()
  }
}
