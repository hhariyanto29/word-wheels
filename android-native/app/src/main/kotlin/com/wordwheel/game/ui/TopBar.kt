package com.wordwheel.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.wordwheel.game.theme.GameColors

@Composable
fun TopBar(coins: Int, found: Int, total: Int, level: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GameColors.TopBarBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Coins
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(GameColors.GemGreen),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = coins.toString(),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.width(18.dp))

        // Words badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GameColors.BadgeBlue)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "W  $found/$total",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
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
