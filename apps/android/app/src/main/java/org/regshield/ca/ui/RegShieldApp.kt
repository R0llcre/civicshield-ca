package org.regshield.ca.ui

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.regshield.ca.R
import org.regshield.ca.ui.components.AppChrome
import org.regshield.ca.ui.components.ChromeNavItem
import org.regshield.ca.ui.components.CivicBackground
import org.regshield.ca.ui.screens.help.HelpScreen
import org.regshield.ca.ui.screens.shield.ShieldScreen
import org.regshield.ca.ui.screens.verify.VerifyScreen
import org.regshield.ca.vault.VaultScreen
import org.regshield.ca.ui.theme.CivicAccentColors
import org.regshield.ca.ui.theme.LocalCivicAccent

private object Routes {
  const val VERIFY = "verify"
  const val SHIELD = "shield"
  const val ASK = "ask"
  const val HELP = "help"
  const val VAULT = "vault"
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun RegShieldApp() {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val destination = backStackEntry?.destination
  val context = LocalContext.current
  val activity = context as? Activity
  val widthSizeClass = activity?.let { calculateWindowSizeClass(activity = it).widthSizeClass }
  val isCompact = widthSizeClass == null || widthSizeClass == WindowWidthSizeClass.Compact

  val rawRoute = destination?.route
  val selectedBottomRoute = if (rawRoute == Routes.VAULT) Routes.SHIELD else rawRoute
  val showBottomBar = rawRoute != Routes.VAULT

  val scheme = MaterialTheme.colorScheme
  val accentColors =
    when (rawRoute) {
      Routes.VERIFY, Routes.ASK, null ->
        CivicAccentColors(
          accent = scheme.primary,
          onAccent = scheme.onPrimary,
          accentContainer = scheme.primaryContainer,
          onAccentContainer = scheme.onPrimaryContainer,
        )
      Routes.SHIELD, Routes.VAULT ->
        CivicAccentColors(
          accent = scheme.secondary,
          onAccent = scheme.onSecondary,
          accentContainer = scheme.secondaryContainer,
          onAccentContainer = scheme.onSecondaryContainer,
        )
      Routes.HELP ->
        CivicAccentColors(
          accent = scheme.tertiary,
          onAccent = scheme.onTertiary,
          accentContainer = scheme.tertiaryContainer,
          onAccentContainer = scheme.onTertiaryContainer,
        )
      else ->
        CivicAccentColors(
          accent = scheme.primary,
          onAccent = scheme.onPrimary,
          accentContainer = scheme.primaryContainer,
          onAccentContainer = scheme.onPrimaryContainer,
        )
    }

  val bottomItems = listOf(
    ChromeNavItem(Routes.VERIFY, stringResource(R.string.nav_verify), R.drawable.ic_verify),
    ChromeNavItem(Routes.SHIELD, stringResource(R.string.nav_shield), R.drawable.ic_shield),
    ChromeNavItem(Routes.ASK, stringResource(R.string.nav_ask), R.drawable.ic_ask),
    ChromeNavItem(Routes.HELP, stringResource(R.string.nav_help), R.drawable.ic_help),
  )

  fun navigateBottom(route: String) {
    navController.navigate(route) {
      launchSingleTop = true
      popUpTo(Routes.VERIFY) { saveState = true }
      restoreState = true
    }
  }

  CompositionLocalProvider(LocalCivicAccent provides accentColors) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
      CivicBackground(modifier = Modifier.fillMaxSize())
      AppChrome(
        isCompact = isCompact,
        currentRoute = selectedBottomRoute,
        items = bottomItems,
        showBottomBar = showBottomBar,
        onNavigate = { route ->
          val alreadySelected =
            destination?.hierarchy?.any { it.route == route } == true ||
              (rawRoute == Routes.VAULT && route == Routes.SHIELD)
          if (!alreadySelected) {
            navigateBottom(route)
          }
        },
      ) { contentPadding ->
        NavHost(
          navController = navController,
          startDestination = Routes.VERIFY,
          modifier =
            Modifier
              .fillMaxSize()
              .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
              ),
        ) {
          composable(Routes.VERIFY) {
            VerifyScreen(
              isCompact = isCompact,
              contentPadding = contentPadding,
            )
          }
          composable(Routes.SHIELD) {
            ShieldScreen(
              isCompact = isCompact,
              contentPadding = contentPadding,
              onOpenVault = { navController.navigate(Routes.VAULT) },
              onGoVerify = { navigateBottom(Routes.VERIFY) },
              onGoHelp = { navigateBottom(Routes.HELP) },
            )
          }
          composable(Routes.ASK) {
            org.regshield.ca.ui.screens.ask.AskScreen(
              isCompact = isCompact,
              contentPadding = contentPadding,
              onGoVerify = { navigateBottom(Routes.VERIFY) },
              onGoHelp = { navigateBottom(Routes.HELP) },
            )
          }
          composable(Routes.HELP) {
            HelpScreen(
              isCompact = isCompact,
              contentPadding = contentPadding,
            )
          }
          composable(Routes.VAULT) {
            VaultScreen(
              isCompact = isCompact,
              contentPadding = contentPadding,
              onClose = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }
}
