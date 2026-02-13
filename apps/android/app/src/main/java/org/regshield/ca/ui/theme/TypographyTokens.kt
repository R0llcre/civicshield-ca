package org.regshield.ca.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.regshield.ca.R

private val CivicDisplayFont =
  FontFamily(
    Font(R.font.space_grotesk_regular, weight = FontWeight.Normal),
    Font(R.font.space_grotesk_medium, weight = FontWeight.Medium),
    Font(R.font.space_grotesk_bold, weight = FontWeight.Bold),
  )

private val CivicBodyFont =
  FontFamily(
    Font(R.font.manrope_regular, weight = FontWeight.Normal),
    Font(R.font.manrope_medium, weight = FontWeight.Medium),
    Font(R.font.manrope_semibold, weight = FontWeight.SemiBold),
  )

val CivicTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = CivicDisplayFont,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.2).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = CivicDisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.1).sp,
  ),
  titleLarge = TextStyle(
    fontFamily = CivicDisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = CivicDisplayFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = CivicBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = CivicBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = CivicBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = CivicBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = CivicBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp,
  ),
)
