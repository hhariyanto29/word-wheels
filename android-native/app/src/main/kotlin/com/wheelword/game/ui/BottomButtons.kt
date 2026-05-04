package com.wheelword.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HintButton(hintsLeft, wordsTowardHint, onHint)
    }
}

@Composable
private fun HintButton(hintsLeft: Int, wordsTowardHint: Int, onHint: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Outer container snug against the 64dp gold disc — 78dp leaves
        // just enough room for the 28dp count badge at the top-right
        // corner with the badge sitting tight against the disc edge.
        // 84dp (the previous value) had ~10dp of awkward gap that read
        // as detached from the button.
        Box(
            modifier = Modifier.size(78.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomStart)
                    .shadow(elevation = 6.dp, shape = CircleShape)
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
            }
            // Count badge — anchored top-right of the outer 84dp box.
            // The 2.5dp white ring + drop shadow makes it pop against
            // the gold disc behind it so the digit is always readable.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (hintsLeft > 0) Color(0xFF32B450)
                        else Color(0xFF787878)
                    )
                    .border(width = 2.5.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hintsLeft.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        // Progress toward the next free hint (every 10 words found).
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .width(72.dp)
                .height(5.dp)
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
