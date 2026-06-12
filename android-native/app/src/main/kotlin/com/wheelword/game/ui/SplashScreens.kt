package com.wheelword.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily
import kotlin.math.cos
import kotlin.math.sin

/*
 * Splash + loading screens from the Word Wheel v2 design handoff
 * (game/screens.jsx): the gold ship-wheel mark, the stacked
 * WORD/WHEEL wordmark, an auto-advancing splash, and a loading
 * screen over the blurred level background with a gold progress bar.
 */

private val StudColors = listOf(
    Color(0xFFE85454), Color(0xFF4D9BE8), Color(0xFF34C26B), Color(0xFFFFC23E),
)

/** Gold ship's wheel: ring, 8 spokes, coloured studs, centre hub. */
@Composable
fun WheelMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 100f   // design drawn on a 100-unit viewbox
        val c = Offset(this.size.width / 2f, this.size.height / 2f)

        // spokes from r30 out to r48 with round caps
        for (i in 0 until 8) {
            val a = (i / 8f) * 2f * Math.PI.toFloat()
            drawLine(
                color = GameColors.Gold,
                start = Offset(c.x + cos(a) * 30f * s, c.y + sin(a) * 30f * s),
                end = Offset(c.x + cos(a) * 48f * s, c.y + sin(a) * 48f * s),
                strokeWidth = 6f * s,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        // main ring
        drawCircle(color = GameColors.Gold, radius = 34f * s, center = c, style = Stroke(width = 9f * s))
        drawCircle(
            color = GameColors.GoldDeep.copy(alpha = 0.6f),
            radius = 34f * s, center = c, style = Stroke(width = 2f * s),
        )
        // coloured handle studs at the spoke tips
        for (i in 0 until 8) {
            val a = (i / 8f) * 2f * Math.PI.toFloat()
            val p = Offset(c.x + cos(a) * 48f * s, c.y + sin(a) * 48f * s)
            drawCircle(color = StudColors[i % 4], radius = 4.5f * s, center = p)
            drawCircle(color = Color.White, radius = 4.5f * s, center = p, style = Stroke(width = 1.4f * s))
        }
        // hub
        drawCircle(color = GameColors.Gold, radius = 11f * s, center = c)
        drawCircle(color = GameColors.GoldDeep, radius = 11f * s, center = c, style = Stroke(width = 2f * s))
        drawCircle(color = GameColors.GoldDeep, radius = 4f * s, center = c)
    }
}

/** Stacked WORD (white) / WHEEL (gold) wordmark. */
@Composable
fun Wordmark(fontSize: Int = 44) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "WORD",
            color = Color.White,
            fontFamily = BalooFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            letterSpacing = 2.sp,
            lineHeight = (fontSize * 1.02f).sp,
            style = TextStyle(
                shadow = Shadow(color = Color(0x40000000), offset = Offset(0f, 6f), blurRadius = 2f),
            ),
        )
        Text(
            text = "WHEEL",
            color = GameColors.Gold,
            fontFamily = BalooFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            letterSpacing = 2.sp,
            lineHeight = (fontSize * 1.02f).sp,
            style = TextStyle(
                shadow = Shadow(color = Color(0x40000000), offset = Offset(0f, 6f), blurRadius = 2f),
            ),
        )
    }
}

/** Slow continuous rotation, like the prototype's wwSpinSlow keyframes. */
@Composable
private fun slowSpin(periodMillis: Int): Float {
    val transition = rememberInfiniteTransition(label = "slowSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "slowSpinAngle",
    )
    return angle
}

/** Dark-navy splash: spinning wheel mark + wordmark + studio tagline. */
@Composable
fun SplashIntroScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1700)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(GameColors.BgTop, GameColors.NavyDeep),
                    radius = 1200f,
                )
            ),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WheelMark(size = 110.dp, modifier = Modifier.rotate(slowSpin(14_000)))
            Spacer(Modifier.height(26.dp))
            Wordmark(fontSize = 46)
        }
        Text(
            text = "A WORD VOYAGE",
            color = GameColors.InkMute,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
        )
    }
}

/**
 * Loading screen: the upcoming level's background blurred + dimmed,
 * wheel mark + wordmark centred, gold shimmer progress bar + tip at
 * the bottom. Calls [onDone] when the (timed) progress completes.
 */
@Composable
fun LoadingScreen(level: Int, onDone: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val durationMs = 1900L
        val start = System.currentTimeMillis()
        while (true) {
            val p = ((System.currentTimeMillis() - start).toFloat() / durationMs).coerceAtMost(1f)
            progress = p
            if (p >= 1f) break
            kotlinx.coroutines.delay(40)
        }
        kotlinx.coroutines.delay(250)
        onDone()
    }

    Box(modifier = Modifier.fillMaxSize().background(GameColors.NavyDeep)) {
        // Blur is a no-op below Android 12; the dark overlay keeps the
        // composition legible there too.
        GameBackgroundImage(
            level = level,
            modifier = Modifier.fillMaxSize().blur(7.dp),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0x73050C1E)))

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WheelMark(size = 88.dp, modifier = Modifier.rotate(slowSpin(10_000)))
            Spacer(Modifier.height(22.dp))
            Wordmark(fontSize = 38)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 48.dp)
                .padding(bottom = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x73000000))
                    .border(1.dp, GameColors.Stroke, RoundedCornerShape(6.dp)),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(GameColors.Gold, GameColors.GoldLight),
                                )
                            ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tip: drag letters on the wheel to connect words",
                color = GameColors.InkSoft,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color(0x99000000), offset = Offset(0f, 2f), blurRadius = 6f),
                ),
            )
        }
    }
}
