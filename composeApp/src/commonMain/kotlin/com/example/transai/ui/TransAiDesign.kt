package com.example.transai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val TransAiColorScheme = darkColorScheme(
    primary = Color(0xFF74E0FF),
    onPrimary = Color(0xFF03121B),
    primaryContainer = Color(0xFF14364A),
    onPrimaryContainer = Color(0xFFD9F7FF),
    secondary = Color(0xFFB794FF),
    onSecondary = Color(0xFF1B1030),
    secondaryContainer = Color(0xFF33214F),
    onSecondaryContainer = Color(0xFFF0E5FF),
    tertiary = Color(0xFF51D4A8),
    onTertiary = Color(0xFF041610),
    tertiaryContainer = Color(0xFF173A30),
    onTertiaryContainer = Color(0xFFD8FFF0),
    background = Color(0xFF07111F),
    onBackground = Color(0xFFF3F7FF),
    surface = Color(0xFF0D1728),
    onSurface = Color(0xFFF3F7FF),
    surfaceVariant = Color(0xFF15233B),
    onSurfaceVariant = Color(0xFFA4B2C8),
    outline = Color(0xFF314764),
    outlineVariant = Color(0xFF1D2A43),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF2A0807)
)

val TransAiShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

object TransAiTokens {
    val BackgroundBase = Color(0xFF08111D)
    val BackgroundTop = Color(0xFF0B1730)
    val BackgroundGlow = Color(0xFF16243D)
    val GlassBorder = Color(0x223A557A)
    val GlassFill = Color(0xEE0F1A2E)
    val GlassFillStrong = Color(0xFF0D1728)
    val AccentCyan = Color(0xFF74E0FF)
    val AccentPurple = Color(0xFFB794FF)
    val AccentMint = Color(0xFF51D4A8)
    val BookCoverStart = Color(0xFF1A2B4A)
    val BookCoverEnd = Color(0xFF21163F)
}

fun auroraBackgroundBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        TransAiTokens.BackgroundTop,
        TransAiTokens.BackgroundBase,
        Color(0xFF050B14)
    )
)

fun heroGlowBrush(): Brush = Brush.radialGradient(
    colors = listOf(
        TransAiTokens.AccentCyan.copy(alpha = 0.08f),
        TransAiTokens.AccentPurple.copy(alpha = 0.05f),
        Color.Transparent
    )
)

fun bookCoverBrush(index: Int): Brush {
    val palette = listOf(
        listOf(Color(0xFF182A4B), Color(0xFF3D2458)),
        listOf(Color(0xFF10314B), Color(0xFF1B5A63)),
        listOf(Color(0xFF2A224E), Color(0xFF503372)),
        listOf(Color(0xFF27314E), Color(0xFF18415D))
    )
    val colors = palette[index % palette.size]
    return Brush.linearGradient(colors)
}

@Composable
fun TransAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TransAiColorScheme,
        typography = com.example.transai.appTypography(),
        shapes = TransAiShapes,
        content = content
    )
}

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(auroraBackgroundBrush()),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(heroGlowBrush())
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = TransAiTokens.GlassBorder,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = TransAiTokens.GlassFill
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

fun ColorScheme.readerCardBackground(): Color = TransAiTokens.GlassFillStrong
