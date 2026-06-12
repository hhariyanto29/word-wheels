package com.wheelword.game.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.wheelword.game.R

// Word Wheel v2 design tokens — mirrors the design handoff's
// game/tokens.jsx (navy glass panels, candy-gold accents).
object GameColors {
    // Base navy palette
    val Navy = Color(0xFF13254C)
    val NavyDeep = Color(0xFF0C1832)
    val PanelTop = Color(0xFF1D3463)
    val PanelSolid = Color(0xFF16284F)
    val Glass = Color(0x9E0A142D)          // rgba(10,20,45,0.62)
    val Stroke = Color(0x24FFFFFF)         // rgba(255,255,255,0.14)
    val StrokeSoft = Color(0x14FFFFFF)     // rgba(255,255,255,0.08)
    val Scrim = Color(0xA8040A1A)          // rgba(4,10,26,0.66)

    // Accents
    val Green = Color(0xFF2EBD6B)
    val GreenDeep = Color(0xFF1F9E54)
    val Gold = Color(0xFFFFB938)
    val GoldDeep = Color(0xFFE89A0C)
    val GoldLight = Color(0xFFFFD66B)
    val Blue = Color(0xFF4D9BE8)
    val BlueDeep = Color(0xFF2F6FBF)
    val Red = Color(0xFFE85454)
    val RedDeep = Color(0xFFC13B3B)

    // Crossword tiles
    val TileEmpty = Color(0xE0FFFFFF)      // rgba(255,255,255,0.88)
    val TileFillA = Color(0xFF3D6FD6)
    val TileFillB = Color(0xFF2A4FA8)
    val BoardPanel = Color(0x8C0A1632)     // rgba(10,22,50,0.55)

    // Ink
    val InkDark = Color(0xFF13254C)
    val InkSoft = Color(0xBFFFFFFF)        // rgba(255,255,255,0.75)
    val InkMute = Color(0x80FFFFFF)        // rgba(255,255,255,0.5)

    // Letter wheel
    val WheelBg = Color(0xEBFFFFFF)        // white disc at 92% opacity
    val LetterColor = Color(0xFF13254C)
    val TileSelectedBg = Color(0xFFFFB938) // gold trace/selection
    val LineColor = Color(0xFFFFB938)
    val ShuffleIcon = Color(0x9913254C)

    // Background gradient behind the level artwork
    val BgTop = Color(0xFF1B3160)
    val BgBottom = Color(0xFF0C1832)
}

// Variable fonts: one TTF per family; the wght axis is pinned per
// declared weight so SemiBold/ExtraBold render as true instances
// (minSdk 26 supports font variation settings).
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Display face — rounded, chunky. Used for wordmarks, buttons, counters. */
val BalooFamily = FontFamily(
    variableFont(R.font.baloo2, FontWeight.Medium),
    variableFont(R.font.baloo2, FontWeight.SemiBold),
    variableFont(R.font.baloo2, FontWeight.Bold),
    variableFont(R.font.baloo2, FontWeight.ExtraBold),
)

/** Body face — UI copy, captions, dialog text. */
val NunitoFamily = FontFamily(
    variableFont(R.font.nunito, FontWeight.SemiBold),
    variableFont(R.font.nunito, FontWeight.Bold),
    variableFont(R.font.nunito, FontWeight.ExtraBold),
)

private val LightColors = lightColorScheme(
    primary = GameColors.Blue,
    background = GameColors.NavyDeep,
    surface = GameColors.PanelSolid,
)

private val DarkColors = darkColorScheme(
    primary = GameColors.Blue,
    background = GameColors.NavyDeep,
    surface = GameColors.PanelSolid,
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
