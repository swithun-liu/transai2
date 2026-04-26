package com.example.transai

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class AppFonts(
    val ui: FontFamily,
    val reading: FontFamily,
    val cjk: FontFamily
)

@Composable
expect fun appFonts(): AppFonts

@Composable
fun appTypography(): Typography {
    val base = Typography()
    val fonts = appFonts()
    return Typography(
        displayLarge = base.displayLarge.withFont(fonts.cjk, FontWeight.SemiBold),
        displayMedium = base.displayMedium.withFont(fonts.cjk, FontWeight.SemiBold),
        displaySmall = base.displaySmall.withFont(fonts.cjk, FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.withFont(fonts.cjk, FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.withFont(fonts.cjk, FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.withFont(fonts.cjk, FontWeight.SemiBold),
        titleLarge = base.titleLarge.withFont(fonts.cjk, FontWeight.SemiBold),
        titleMedium = base.titleMedium.withFont(fonts.cjk, FontWeight.SemiBold),
        titleSmall = base.titleSmall.withFont(fonts.cjk, FontWeight.Medium),
        bodyLarge = base.bodyLarge.withFont(fonts.cjk, FontWeight.Normal),
        bodyMedium = base.bodyMedium.withFont(fonts.cjk, FontWeight.Normal),
        bodySmall = base.bodySmall.withFont(fonts.cjk, FontWeight.Normal),
        labelLarge = base.labelLarge.withFont(fonts.cjk, FontWeight.Medium),
        labelMedium = base.labelMedium.withFont(fonts.cjk, FontWeight.Medium),
        labelSmall = base.labelSmall.withFont(fonts.cjk, FontWeight.Medium)
    )
}

private fun TextStyle.withFont(
    fontFamily: FontFamily,
    fontWeight: FontWeight
): TextStyle = copy(
    fontFamily = fontFamily,
    fontWeight = fontWeight
)
