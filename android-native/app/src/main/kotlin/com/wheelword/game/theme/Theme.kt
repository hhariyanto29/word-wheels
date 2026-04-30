package com.wheelword.game.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mirror of color constants from core/src/lib.rs:402-417
object GameColors {
    val BgTop = Color(0xFF3C64C8)
    val BgBottom = Color(0xFF8CB4F0)
    val CellFilled = Color(0xFF193778)
    val CellEmpty = Color(0xFFC8D7F0)
    val CellFoundText = Color.White
    val TopBarBg = Color(0xDC142850)
    val GemGreen = Color(0xFF32C850)
    val BadgeBlue = Color(0xFF50A0E6)
    val WheelBg = Color(0xE6FFFFFF)
    val TileSelectedBg = Color(0xFF50A0E6)
    val LineColor = Color(0xFF50A0E6)
    val LetterColor = Color(0xFF1E1E1E)
    val HintBtnBg = Color(0xB4282828)
    val ShuffleIcon = Color(0xFF96A0AF)
    val SubmitBg = Color(0xFF50A0E6)
    val SubmitHover = Color(0xFF3C8CD2)
    val GridBackdrop = Color(0xB40A193C)
    val StarYellow = Color(0xFFFFDC50)
    val CompleteBorder = Color(0xFF64A0FF)
    val CompleteBg = Color(0xF5142850)
}

private val LightColors = lightColorScheme(
    primary = GameColors.BgTop,
    background = GameColors.BgTop,
    surface = GameColors.TopBarBg,
)

private val DarkColors = darkColorScheme(
    primary = GameColors.BgTop,
    background = GameColors.BgTop,
    surface = GameColors.TopBarBg,
)

@Composable
fun WordWheelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
