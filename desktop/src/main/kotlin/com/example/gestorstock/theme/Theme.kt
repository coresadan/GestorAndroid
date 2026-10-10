package com.example.gestorstock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Rosa,
    onPrimary = Color.White,
    primaryContainer = RosaOscuro,
    onPrimaryContainer = RosaClaro,
    secondary = Morado,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3D1F70),
    onSecondaryContainer = MoradoClaro,
    background = FondoOscuro,
    onBackground = TextoPrimario,
    surface = FondoCard,
    onSurface = TextoPrimario,
    surfaceVariant = FondoCardElevado,
    onSurfaceVariant = TextoSecundario,
    error = Rojo,
    outline = Color(0xFF4A3660),
    outlineVariant = Color(0xFF2E1F45)
)

@Composable
fun GestorStockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
