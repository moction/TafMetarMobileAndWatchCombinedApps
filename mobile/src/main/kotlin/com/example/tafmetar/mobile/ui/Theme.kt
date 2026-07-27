package com.example.tafmetar.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Gris utilisé pour le texte secondaire : lisible sur noir sans concurrencer le texte principal. */
val DimGrey = Color(0xFF9E9E9E)

/**
 * Palette minimaliste : uniquement du noir, du blanc et un gris. Aucune couleur d'accent —
 * la hiérarchie visuelle repose sur le contraste et la taille du texte.
 *
 * `background` et `surface` sont identiques (noir pur) pour qu'aucun composant Material ne
 * fasse apparaître un fond légèrement plus clair que le reste de l'écran.
 */
private val MinimalDarkColors = darkColorScheme(
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = DimGrey,
    primary = Color.White,
    onPrimary = Color.Black,
    outline = DimGrey,
    error = Color.White
)

@Composable
fun TafMetarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MinimalDarkColors, content = content)
}
