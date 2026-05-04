package com.wheelword.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.GameColors

/**
 * "How to Play" modal. Shown automatically on first launch (gated by
 * [com.wheelword.game.storage.GameStorage.seenHelp]) and via the `?`
 * icon in the TopBar afterwards.
 */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
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
            Text(
                text = "How to Play",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))

            HelpItem(
                glyph = "✋",
                title = "Drag to spell",
                body = "Drag your finger across the wheel letters to build a word. Release to submit.",
            )
            HelpItem(
                glyph = "✓",
                title = "Fill the grid",
                body = "Words you find that fit the crossword fill in. Solve them all to clear the level.",
            )
            HelpItem(
                glyph = "★",
                title = "Bonus words",
                body = "Extra valid words still earn points — they appear in the row below the wheel.",
            )
            HelpItem(
                glyph = "💡",
                title = "Hints",
                body = "Tap the hint button to reveal one grid letter. Earn +1 hint every 10 words you find.",
            )
            HelpItem(
                glyph = "🔄",
                title = "Shuffle",
                body = "Tap the centre of the wheel to shuffle the tiles when you're stuck.",
            )

            Spacer(Modifier.height(20.dp))
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
private fun HelpItem(glyph: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = glyph,
            fontSize = 22.sp,
            modifier = Modifier
                .width(36.dp)
                .padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                color = Color(0xC0FFFFFF),
                fontSize = 13.sp,
            )
        }
    }
}
