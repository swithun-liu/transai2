package com.example.transai

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import transai.composeapp.generated.resources.SourceSans3_Regular
import transai.composeapp.generated.resources.SourceSans3_Semibold
import transai.composeapp.generated.resources.SourceSerif4_Regular
import transai.composeapp.generated.resources.SourceSerif4_Semibold
import transai.composeapp.generated.resources.Res
import transai.composeapp.generated.resources.NotoSansSC_Regular

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun appFonts(): AppFonts = AppFonts(
    ui = FontFamily(
        Font(Res.font.SourceSans3_Regular, weight = FontWeight.Normal),
        Font(Res.font.SourceSans3_Semibold, weight = FontWeight.SemiBold),
        Font(Res.font.NotoSansSC_Regular, weight = FontWeight.Normal)
    ),
    reading = FontFamily(
        Font(Res.font.SourceSerif4_Regular, weight = FontWeight.Normal),
        Font(Res.font.SourceSerif4_Semibold, weight = FontWeight.SemiBold),
        Font(Res.font.NotoSansSC_Regular, weight = FontWeight.Normal)
    ),
    cjk = FontFamily(
        Font(Res.font.NotoSansSC_Regular, weight = FontWeight.Normal)
    )
)
