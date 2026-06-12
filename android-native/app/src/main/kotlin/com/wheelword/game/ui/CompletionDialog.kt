package com.wheelword.game.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors

/**
 * Level-complete celebration in the Word Wheel v2 design: dim navy
 * overlay, three stars popping in one after another, the headline,
 * the coin bonus line, and a green candy CONTINUE button.
 */
@Composable
fun CompletionDialog(
    levelNum: Int,
    isLastLevel: Boolean,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PopStar(sizeSp = 42, delayMillis = 150L)
                Spacer(Modifier.width(10.dp))
                PopStar(sizeSp = 56, delayMillis = 330L)
                Spacer(Modifier.width(10.dp))
                PopStar(sizeSp = 42, delayMillis = 510L)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (isLastLevel) "All Levels Complete!" else "Level $levelNum Complete!",
                color = Color.White,
                fontFamily = BalooFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color(0x4D000000), offset = Offset(0f, 6f), blurRadius = 2f),
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+10 ",
                    color = GameColors.Gold,
                    fontFamily = BalooFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                )
                StarCoinIcon(size = 18.dp)
                Text(
                    text = " bonus",
                    color = GameColors.Gold,
                    fontFamily = BalooFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                )
            }
            Spacer(Modifier.height(26.dp))
            CandyButton(
                text = if (isLastLevel) "PLAY AGAIN (LV.1)" else "CONTINUE",
                onClick = onNext,
                color = GameColors.Green,
                colorDeep = GameColors.GreenDeep,
                fontSize = 22.sp,
                modifier = Modifier.width(250.dp),
            )
        }
    }
}

/** A star emoji that springs in after [delayMillis] (wwStarPop). */
@Composable
private fun PopStar(sizeSp: Int, delayMillis: Long) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis)
        shown = true
    }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "starPop",
    )
    Text(
        text = "⭐",
        fontSize = sizeSp.sp,
        modifier = Modifier.scale(scale),
    )
}
