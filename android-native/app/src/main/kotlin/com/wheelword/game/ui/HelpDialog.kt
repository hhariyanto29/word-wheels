package com.wheelword.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily

/**
 * "How to Play" modal. Shown automatically on first launch (gated by
 * [com.wheelword.game.storage.GameStorage.seenHelp]) and via the `?`
 * icon afterwards.
 */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    ModalShell(onDismiss = onDismiss, widthFraction = 0.88f) {
        Text(
            text = "How to Play",
            color = Color.White,
            fontFamily = BalooFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(14.dp))

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
        CandyButton(
            text = "Got it",
            onClick = onDismiss,
            color = GameColors.Blue,
            colorDeep = GameColors.BlueDeep,
            fontSize = 17.sp,
            modifier = Modifier.fillMaxWidth(),
            verticalPadding = 11.dp,
        )
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
                fontFamily = BalooFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                color = GameColors.InkSoft,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}
