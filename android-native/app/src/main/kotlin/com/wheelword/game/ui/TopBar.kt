package com.wheelword.game.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors

/**
 * In-puzzle HUD: a row of floating glass pills (coins, W-count,
 * streak on the left; level on the right) in the Word Wheel v2
 * design language. No longer a single solid bar — each stat is its
 * own glass capsule.
 */
@Composable
fun TopBar(
    coins: Int,
    found: Int,
    total: Int,
    level: Int,
    streak: Int = 0,
    onCoinClick: (() -> Unit)? = null,
) {
    // Tween-animate the displayed coin count when it changes — the spin
    // reward (and word-completion +2) ticks up smoothly instead of
    // snapping.
    val displayedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "coinCounter",
    )
    // Pulse the coin icon when the value jumps by a meaningful amount
    // (skip the +2 from a single word).
    var lastCoins by remember { mutableStateOf(coins) }
    var pulseActive by remember { mutableStateOf(false) }
    LaunchedEffect(coins) {
        if (coins - lastCoins >= 5) {
            pulseActive = true
            kotlinx.coroutines.delay(240)
            pulseActive = false
        }
        lastCoins = coins
    }
    val pulse by animateFloatAsState(
        targetValue = if (pulseActive) 1.25f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "coinPulse",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HUDPill(onClick = onCoinClick) {
            StarCoinIcon(size = 22.dp, modifier = Modifier.scale(pulse))
            Spacer(Modifier.width(7.dp))
            Text(text = displayedCoins.toString(), style = HUDPillTextStyle)
        }

        Spacer(Modifier.width(6.dp))

        HUDPill {
            Text(
                text = "W",
                color = Color.White,
                fontFamily = BalooFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GameColors.Blue)
                    .padding(horizontal = 8.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "$found/$total",
                style = HUDPillTextStyle,
                fontSize = 15.sp,
            )
        }

        // Streak pill — only once a streak is going, to avoid noise on
        // first install.
        if (streak > 0) {
            Spacer(Modifier.width(6.dp))
            HUDPill {
                Text(text = "🔥", fontSize = 15.sp)
                Spacer(Modifier.width(5.dp))
                Text(text = streak.toString(), style = HUDPillTextStyle, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        HUDPill {
            Text(
                text = "Lv.$level",
                style = HUDPillTextStyle,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
