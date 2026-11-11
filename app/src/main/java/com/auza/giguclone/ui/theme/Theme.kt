package com.auza.giguclone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryDark = Color(0xFF00E676)
val SecondaryDark = Color(0xFF2979FF)
val BackgroundDark = Color(0xFF101216)
val SurfaceDark = Color(0xFF1B1E24)
val CardBackground = Color(0xFF232730)

val SemafaroGreen = Color(0xFF00C853)
val SemafaroYellow = Color(0xFFFFD600)
val SemafaroRed = Color(0xFFD50000)
val SemafaroPurple = Color(0xFF9C27B0) // Alerta de Área de Risco

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun GigUCloneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
