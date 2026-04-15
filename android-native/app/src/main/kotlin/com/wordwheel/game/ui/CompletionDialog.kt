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
import com.wordwheel.game.theme.GameColors

@Composable
fun CompletionDialog(isLastLevel: Boolean, onNext: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x78000000)),
        contentAlignment = Alignment.Center,
    ) {
        // Cap width at the smaller viewport edge (works for tall phones,
        // landscape, and small tablets) while never exceeding 320dp.
        val dialogWidth = minOf(maxWidth, maxHeight) - 48.dp
        val finalWidth = dialogWidth.coerceIn(220.dp, 320.dp)
        Column(
            modifier = Modifier
                .width(finalWidth)
                .clip(RoundedCornerShape(16.dp))
                .background(GameColors.CompleteBg)
                .border(
                    width = 2.dp,
                    color = GameColors.CompleteBorder,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isLastLevel) "All Levels Complete!" else "Level Complete!",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "+10 pts bonus!",
                color = GameColors.StarYellow,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(19.dp))
                    .background(Color(0xFF28B446))
                    .clickable { onNext() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    text = if (isLastLevel) "Play Again (Lv.1)" else "Next Level",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
