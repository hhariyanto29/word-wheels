package com.wheelword.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.HINT_COIN_COST
import com.wheelword.game.theme.GameColors

/**
 * Modal explaining the coin economy. Triggered from the coin badge in
 * [TopBar]. The "Buy a hint for [HINT_COIN_COST] points" line is the
 * point of the dialog — players who run out of free hints need to know
 * they can keep advancing by spending coins.
 */
@Composable
fun CoinInfoDialog(coins: Int, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(GameColors.CompleteBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Big gold coin
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFE070), Color(0xFFFFB020)),
                        )
                    )
                    .border(width = 2.dp, color = Color(0xFFB37400), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "★",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Points",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "You have $coins",
                color = Color(0xC0FFFFFF),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(18.dp))

            CoinInfoRow(
                glyph = "✏️",
                title = "Find words",
                detail = "+2 points per word — grid answers and bonus words.",
            )
            CoinInfoRow(
                glyph = "🏆",
                title = "Finish a level",
                detail = "+10 bonus when you complete the crossword.",
            )
            CoinInfoRow(
                glyph = "🎁",
                title = "Daily spin",
                detail = "Spin the wheel once a day for a coin / hint reward.",
            )
            CoinInfoRow(
                glyph = "💡",
                title = "Buy a hint for $HINT_COIN_COST",
                detail = "When your free hints are gone, tap the lightbulb to spend $HINT_COIN_COST points and reveal a letter.",
            )

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF50A0E6))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 36.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Got it",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CoinInfoRow(glyph: String, title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = glyph,
            fontSize = 20.sp,
            modifier = Modifier
                .width(32.dp)
                .padding(top = 1.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                color = Color(0xB8FFFFFF),
                fontSize = 12.sp,
            )
        }
    }
}
