package com.wordwheel.game.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.theme.GameColors

/** Layered coin glyph. Two concentric circles + a centred star give a
 *  more "game-y" point-counter than the plain green disc that came
 *  before. Drawn entirely in Compose so we don't need a drawable
 *  resource and it scales cleanly on every density. */
@Composable
private fun CoinIcon(modifier: Modifier = Modifier, size: Int = 22) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFE070), Color(0xFFFFAA00)),
                )
            )
            .border(width = 2.dp, color = Color(0xFF8B5A00), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "★",
            color = Color(0xFF8B5A00),
            fontSize = (size * 0.6f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun TopBar(coins: Int, found: Int, total: Int, level: Int, streak: Int = 0) {
    // Tween-animate the displayed coin count when it changes — the spin
    // reward (and word-completion +2) ticks up smoothly instead of
    // snapping. ~600ms is short enough to feel responsive but long
    // enough to read the change.
    val displayedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "coinCounter",
    )
    // Track previous count so we can pulse the icon when the value
    // jumps by a meaningful amount (skip the +2 from a single word).
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GameColors.TopBarBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(modifier = Modifier.scale(pulse), size = 22)
        Spacer(Modifier.width(8.dp))
        Text(
            text = displayedCoins.toString(),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.width(14.dp))

        // Words badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GameColors.BadgeBlue)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "W  $found/$total",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Streak badge — only shown once the player has a streak going.
        // Hidden at streak==0 to avoid noise on first install.
        if (streak > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x40FF7028))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "🔥 $streak",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Level badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x28FFFFFF))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "Lv.$level",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
