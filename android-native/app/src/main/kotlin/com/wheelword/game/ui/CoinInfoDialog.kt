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
import com.wheelword.game.HINT_COIN_COST
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily

/**
 * Modal explaining the coin economy. Triggered from the coin pill in
 * the HUD. The "Buy a hint for [HINT_COIN_COST] points" line is the
 * point of the dialog — players who run out of free hints need to know
 * they can keep advancing by spending coins.
 */
@Composable
fun CoinInfoDialog(coins: Int, onDismiss: () -> Unit) {
    ModalShell(onDismiss = onDismiss, widthFraction = 0.88f) {
        StarCoinIcon(size = 56.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Points",
            color = Color.White,
            fontFamily = BalooFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "You have $coins",
            color = GameColors.InkSoft,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))

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
                fontFamily = BalooFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = detail,
                color = GameColors.InkSoft,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
}
