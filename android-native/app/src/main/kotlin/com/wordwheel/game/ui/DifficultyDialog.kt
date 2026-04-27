package com.wordwheel.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.Difficulty
import com.wordwheel.game.theme.GameColors

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(20.dp))
                .background(GameColors.CompleteBg)
                .border(width = 3.dp, color = accent, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "DIFFICULTY UNLOCKED",
                color = Color(0xA0FFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = tier.displayName.uppercase(),
                color = accent,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = tier.tagline,
                color = Color.White,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(accent)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "CONTINUE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
