package com.wheelword.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily

/**
 * Pre-game landing screen, Word Wheel v2 design: full-bleed country
 * background with legibility gradients, glass HUD pills (coins +
 * streak) top-left, glass settings cog top-right, a pulsing green
 * candy "LEVEL X" button low-centre, and the gold daily-spin candy
 * button at the bottom when a spin is available.
 */
@Composable
fun HomeScreen(
    levelNum: Int,
    coins: Int,
    streak: Int,
    spinAvailable: Boolean,
    onResume: () -> Unit,
    onSpinClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GameColors.BgTop, GameColors.BgBottom),
                ),
            ),
    ) {
        // The upcoming level's country artwork teases what the player
        // is about to enter.
        GameBackgroundImage(level = levelNum)
        // Top + bottom legibility gradient (design: dark at the very
        // top and bottom, clear in the middle so the artwork shows).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x6B050C1E),
                        0.18f to Color.Transparent,
                        0.52f to Color.Transparent,
                        1f to Color(0x8C050C1E),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // HUD — coins + streak left, settings right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HUDPill {
                    StarCoinIcon(size = 24.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(text = coins.toString(), style = HUDPillTextStyle)
                }
                if (streak > 0) {
                    Spacer(Modifier.width(8.dp))
                    HUDPill {
                        Text(text = "🔥", fontSize = 17.sp)
                        Spacer(Modifier.width(5.dp))
                        Text(text = streak.toString(), style = HUDPillTextStyle)
                    }
                }
                Spacer(Modifier.weight(1f))
                RoundGlassButton(onClick = onSettingsClick) {
                    Text(text = "⚙", color = Color.White, fontSize = 20.sp)
                }
            }

            // Level CTA — pulsing candy button + caption
            val pulseTransition = rememberInfiniteTransition(label = "ctaPulse")
            val pulse by pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "ctaPulseScale",
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 168.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CandyButton(
                    text = "LEVEL $levelNum",
                    onClick = onResume,
                    color = GameColors.Green,
                    colorDeep = GameColors.GreenDeep,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .scale(pulse)
                        .width(270.dp),
                    verticalPadding = 15.dp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Tap to continue",
                    color = Color.White,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onResume),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xB3000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f,
                        ),
                    ),
                )
            }

            // Daily spin entry — appears when today's spin is unclaimed
            if (spinAvailable) {
                CandyButton(
                    text = "🎁 Daily spin available",
                    onClick = onSpinClick,
                    color = GameColors.Gold,
                    colorDeep = GameColors.GoldDeep,
                    textColor = Color(0xFF5C3A00),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 44.dp),
                    horizontalPadding = 34.dp,
                    verticalPadding = 12.dp,
                )
            }
        }
    }
}
