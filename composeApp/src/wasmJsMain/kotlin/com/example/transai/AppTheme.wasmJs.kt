package com.example.transai

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import transai.composeapp.generated.resources.Res
import transai.composeapp.generated.resources.NotoSansSC_Regular

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun appTypography(): Typography {
    val cjkFontFamily = FontFamily(Font(Res.font.NotoSansSC_Regular))
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = cjkFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = cjkFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = cjkFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = cjkFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = cjkFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = cjkFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = cjkFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = cjkFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = cjkFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = cjkFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = cjkFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = cjkFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = cjkFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = cjkFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = cjkFontFamily)
    )
}
