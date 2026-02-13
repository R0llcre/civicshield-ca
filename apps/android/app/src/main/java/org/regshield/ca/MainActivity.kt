package org.regshield.ca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.regshield.ca.ui.RegShieldApp
import org.regshield.ca.ui.theme.RegShieldTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      RegShieldTheme {
        RegShieldApp()
      }
    }
  }
}
