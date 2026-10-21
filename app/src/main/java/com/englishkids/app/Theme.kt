package com.englishkids.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KSky = Color(0xFFDFF3FF)
val KBlue = Color(0xFF4F9DFF)
val KGreen = Color(0xFF3CC47C)
val KPink = Color(0xFFFF6FA3)
val KSun = Color(0xFFFFC93C)
val KPurple = Color(0xFF8E6BFF)
val KOrange = Color(0xFFFF8A3D)
val KRed = Color(0xFFFF5C5C)
val KInk = Color(0xFF2B2D42)

@Composable
fun KidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = KBlue, secondary = KPink, background = KSky, surface = Color.White),
        content = content
    )
}
