package com.wheelword.game.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.Difficulty
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily

/**
 * Modal banner shown after the player completes a milestone level
 * (10, 20, 40, 60, 80). Briefly announces the next difficulty tier
 * with its accent colour and tagline; tap-anywhere dismisses.
 */
@Composable
fun DifficultyDialog(
    tier: Difficulty,
    onDismiss: () -> Unit,
) {
    val accent = Color(tier.accentHex)

    ModalShell(onDismiss = onDismiss) {
        Text(
            text = "DIFFICULTY UNLOCKED",
            color = GameColors.InkMute,
            fontFamily = NunitoFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = tier.displayName.uppercase(),
            color = accent,
            fontFamily = BalooFamily,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = tier.tagline,
            color = Color.White,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        CandyButton(
            text = "CONTINUE",
            onClick = onDismiss,
            color = accent,
            colorDeep = accent.darken(),
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalPadding = 12.dp,
        )
    }
}

/** Shift a colour ~25% toward black, for candy-button gradients. */
private fun Color.darken(): Color = Color(
    red = red * 0.75f,
    green = green * 0.75f,
    blue = blue * 0.75f,
    alpha = alpha,
)
