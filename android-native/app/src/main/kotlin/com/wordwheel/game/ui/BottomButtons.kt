package com.wordwheel.game.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.theme.GameColors

@Composable
fun BottomButtons(
    hintsLeft: Int,
    wordsTowardHint: Int,
    onHint: () -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HintButton(hintsLeft, wordsTowardHint, onHint)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GameColors.SubmitBg)
                .clickable { onSubmit() }
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Submit",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(GameColors.HintBtnBg)
                .clickable { onBackspace() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u232B",
                color = Color.White,
                fontSize = 22.sp,
            )
        }
    }
}

@Composable
private fun HintButton(hintsLeft: Int, wordsTowardHint: Int, onHint: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(GameColors.HintBtnBg)
                .clickable { onHint() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uD83D\uDCA1",
                fontSize = 22.sp,
            )
            // Hint count badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (hintsLeft > 0) Color(0xFF32B450) else Color(0xFF787878)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hintsLeft.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Progress bar
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(54.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF28283C)),
        ) {
            val progress = (wordsTowardHint.coerceAtMost(10)) / 10f
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFF50DC78)),
                )
            }
        }
    }
}
