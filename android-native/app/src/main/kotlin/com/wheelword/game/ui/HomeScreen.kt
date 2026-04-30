package com.wheelword.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.R
import com.wheelword.game.theme.GameColors

/**
 * Pre-game landing screen. Shows the logo + a big "LEVEL X" button
 * that resumes wherever the player left off, plus a streak/coins
 * stat row across the top and a settings cog top-right.
 *
 * Behaviour mirrors what the Words of Wonders walkthrough showed —
 * the player taps LEVEL X to enter the puzzle screen.
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
        Image(
            painter = painterResource(R.drawable.game_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66000014),
                            Color(0x55000028),
                            Color(0x80000032),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar — coins, streak, settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GameColors.TopBarBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(GameColors.GemGreen),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = coins.toString(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (streak > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x66FF7028))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                        .clickable(onClick = onSettingsClick)
                        .padding(8.dp),
                ) {
                    Text(text = "⚙", color = Color.White, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // Title
            Text(
                text = "WORD",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "WHEEL",
                color = GameColors.StarYellow,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
            )

            Spacer(Modifier.weight(1f))

            // Big resume button — equivalent to "LEVEL 120" in the WoW
            // home screen. Goes straight back into wherever you left off.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFF32C850))
                    .clickable(onClick = onResume)
                    .padding(horizontal = 60.dp, vertical = 18.dp),
            ) {
                Text(
                    text = "LEVEL $levelNum",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Tap to continue",
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp,
            )

            Spacer(Modifier.weight(1f))

            // Daily spin entry — appears when claim is available
            if (spinAvailable) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFFFB400))
                        .clickable(onClick = onSpinClick)
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "🎁  Daily spin available",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
