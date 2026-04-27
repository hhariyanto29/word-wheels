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

/**
 * Bottom action bar.
 *
 * Drag-on-the-wheel is the primary submit gesture — releasing the drag
 * commits the selected word automatically (see [LetterWheel]). That
 * makes both Submit and Backspace buttons redundant. Only the Hint
 * button lives here now, centred and large.
 */
@Composable
fun BottomButtons(
    hintsLeft: Int,
    wordsTowardHint: Int,
    onHint: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HintButton(hintsLeft, wordsTowardHint, onHint)
    }
}

@Composable
private fun HintButton(hintsLeft: Int, wordsTowardHint: Int, onHint: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (hintsLeft > 0) Color(0xFFFFB400)
                    else Color(0xB4282828)
                )
                .clickable(onClick = onHint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uD83D\uDCA1",
                fontSize = 28.sp,
            )
            // Hint count badge in the corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-4).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (hintsLeft > 0) Color(0xFF32B450)
                        else Color(0xFF787878)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hintsLeft.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Progress toward the next free hint (every 10 words found).
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(80.dp)
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
