package com.viaword.demo.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val colors = lightColorScheme(
    primary = Color(0xFF234CCF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EEFF),
    onPrimaryContainer = Color(0xFF171A22),
    background = Color.White,
    onBackground = Color(0xFF171A22),
    surface = Color.White,
    onSurface = Color(0xFF171A22),
    onSurfaceVariant = Color(0xFF4B5563),
    outlineVariant = Color(0xFFDCE1EA),
)

internal val pageMargin = 20.dp
internal val corner = RoundedCornerShape(12.dp)
private val defaultType = Typography()
private val typography = defaultType.copy(
    headlineMedium = defaultType.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = defaultType.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = defaultType.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = defaultType.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
)

@Composable
fun ViawordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(medium = corner),
        content = content,
    )
}
