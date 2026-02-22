package org.regshield.ca.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.regshield.ca.ui.theme.LocalCivicAccent

@Composable
fun CivicTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  singleLine: Boolean = true,
  minLines: Int = if (singleLine) 1 else 3,
  maxLines: Int = if (singleLine) 1 else 6,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  trailingIcon: @Composable (() -> Unit)? = null,
  testTag: String? = null,
) {
  val accent = LocalCivicAccent.current
  var focused by remember { mutableStateOf(false) }
  val borderColor by
    animateColorAsState(
      targetValue =
        if (focused) {
          accent.accent.copy(alpha = 0.95f)
        } else {
          MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        },
      label = "civicTextFieldBorder",
    )

  Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      border = BorderStroke(width = 1.dp, color = borderColor),
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
    ) {
      TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        placeholder = { if (!placeholder.isNullOrBlank()) Text(text = placeholder) },
        trailingIcon = trailingIcon,
        modifier =
          Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .then(if (!testTag.isNullOrBlank()) Modifier.testTag(testTag) else Modifier),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            errorIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
          ),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicDropdownField(
  label: String,
  options: List<String>,
  value: String,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  testTag: String? = null,
  display: (String) -> String = { it },
) {
  var expanded by remember { mutableStateOf(false) }
  val shown = if (value.isBlank()) (placeholder ?: "") else display(value)

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.fillMaxWidth(),
  ) {
    CivicTextField(
      value = shown,
      onValueChange = {},
      label = label,
      readOnly = true,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier =
        Modifier
          .menuAnchor()
          .fillMaxWidth(),
      testTag = testTag,
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      for (opt in options) {
        DropdownMenuItem(
          text = { Text(text = if (opt.isBlank()) (placeholder ?: "") else display(opt)) },
          onClick = {
            onSelect(opt)
            expanded = false
          },
        )
      }
    }
  }
}
