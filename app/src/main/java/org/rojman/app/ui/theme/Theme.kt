package org.rojman.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF9D1C23),
    secondary = Color(0xFF333333),
    tertiary = Color(0xFFB57F00)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7B7B),
    secondary = Color(0xFFE5E5E5),
    tertiary = Color(0xFFFFD35A)
)

@Composable
fun RojmanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
