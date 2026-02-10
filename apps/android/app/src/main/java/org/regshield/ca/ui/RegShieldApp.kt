package org.regshield.ca.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.regshield.ca.R
import org.regshield.ca.ui.screens.help.HelpScreen
import org.regshield.ca.ui.screens.shield.ShieldScreen
import org.regshield.ca.ui.screens.verify.VerifyScreen
import org.regshield.ca.vault.VaultScreen

private object Routes {
  const val VERIFY = "verify"
  const val SHIELD = "shield"
  const val HELP = "help"
  const val VAULT = "vault"
}

private data class BottomItem(
  val route: String,
  val label: String,
  val iconRes: Int,
)

@Composable
fun RegShieldApp() {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val destination = backStackEntry?.destination

  val bottomItems = listOf(
    BottomItem(Routes.VERIFY, "Verify", R.drawable.ic_verify),
    BottomItem(Routes.SHIELD, "Shield", R.drawable.ic_shield),
    BottomItem(Routes.HELP, "Help", R.drawable.ic_help),
  )

  val showBottom = when (destination?.route) {
    Routes.VERIFY, Routes.SHIELD, Routes.HELP, Routes.VAULT -> true
    else -> true
  }

  Scaffold(
    bottomBar = {
      if (!showBottom) return@Scaffold
      BottomAppBar {
        bottomItems.forEach { item ->
          val selected = destination?.hierarchy?.any { it.route == item.route } == true ||
            (destination?.route == Routes.VAULT && item.route == Routes.SHIELD)
          NavigationBarItem(
            selected = selected,
            onClick = {
              navController.navigate(item.route) {
                launchSingleTop = true
                popUpTo(Routes.VERIFY) { saveState = true }
                restoreState = true
              }
            },
            icon = { Icon(painterResource(item.iconRes), contentDescription = item.label) },
            label = { Text(item.label) },
          )
        }
      }
    },
  ) { padding ->
    NavHost(
      navController = navController,
      startDestination = Routes.VERIFY,
      modifier = Modifier.padding(padding),
    ) {
      composable(Routes.VERIFY) { VerifyScreen() }
      composable(Routes.SHIELD) { ShieldScreen(onOpenVault = { navController.navigate(Routes.VAULT) }) }
      composable(Routes.HELP) { HelpScreen() }
      composable(Routes.VAULT) { VaultScreen(onClose = { navController.popBackStack() }) }
    }
  }
}
