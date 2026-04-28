package com.wordwheel.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.theme.GameColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One sector of the lottery wheel.
 *
 * The drawn sector size is uniform (360° / N); [weight] only controls
 * how often the spin lands on that sector.
 */
data class SpinSector(
    val coins: Int,
    val hints: Int,
    val color: Color,
    val weight: Int,
) {
    val label: String = when {
        coins > 0 && hints > 0 -> "+$coins\n+$hints💡"
        coins > 0 -> "+$coins"
        hints > 0 -> "+$hints💡"
        else -> "—"
    }
}

/** The default 8-sector wheel. Casual-friendly: small wins are common,
 *  rarer 50-coin or 3-hint jackpots keep the spin exciting. */
val DEFAULT_SECTORS: List<SpinSector> = listOf(
    SpinSector(coins = 5,  hints = 0, color = Color(0xFF50A0E6), weight = 28),
    SpinSector(coins = 10, hints = 0, color = Color(0xFFFFB400), weight = 22),
    SpinSector(coins = 15, hints = 0, color = Color(0xFF50A0E6), weight = 16),
    SpinSector(coins = 0,  hints = 1, color = Color(0xFF32C850), weight = 12),
    SpinSector(coins = 20, hints = 0, color = Color(0xFFFFB400), weight = 10),
    SpinSector(coins = 25, hints = 0, color = Color(0xFF50A0E6), weight = 6),
    SpinSector(coins = 50, hints = 0, color = Color(0xFFE65050), weight = 4),
    SpinSector(coins = 0,  hints = 3, color = Color(0xFF32C850), weight = 2),
)

/**
 * Modal lottery-wheel screen. Tap "SPIN" once → wheel animates to a
 * weighted-random sector → reward is reported via [onSpinResult].
 *
 * After the spin, tapping the dimmed background or the result button
 * dismisses the dialog.
 */
@Composable
fun SpinWheelDialog(
    sectors: List<SpinSector> = DEFAULT_SECTORS,
    onSpinResult: (SpinSector) -> Unit,
    onDismiss: () -> Unit,
) {
    var hasSpun by rememberSaveable { mutableStateOf(false) }
    var resultIndex by rememberSaveable { mutableIntStateOf(-1) }
    // Pair<finalRotationDegrees, sectorIndex> — set once the user taps
    // SPIN; observed by the LaunchedEffect below.
    var rotationTarget by remember { mutableStateOf<Pair<Float, Int>?>(null) }
    val rotation = remember { Animatable(0f) }

    val measurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8000000))
            .clickable(enabled = hasSpun, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(GameColors.CompleteBg)
                .padding(20.dp),
        ) {
            Text(
                text = if (hasSpun) "Nice!" else "Daily Spin",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasSpun)
                    sectors.getOrNull(resultIndex)?.let { sec ->
                        when {
                            sec.coins > 0 && sec.hints > 0 ->
                                "+${sec.coins} coins and +${sec.hints} hints"
                            sec.coins > 0 -> "+${sec.coins} coins"
                            sec.hints > 0 -> "+${sec.hints} hint${if (sec.hints > 1) "s" else ""}"
                            else -> ""
                        }
                    } ?: ""
                else "Tap below to spin — once per day",
                color = GameColors.StarYellow,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                // Wheel
                Canvas(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .rotate(rotation.value),
                ) {
                    val radius = size.minDimension / 2f * 0.96f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val sweep = 360f / sectors.size

                    for ((i, sector) in sectors.withIndex()) {
                        // Sector centred at the top when its midpoint angle
                        // is -90°, so start = i*sweep − 90° − sweep/2.
                        val start = i * sweep - 90f - sweep / 2f
                        val path = Path().apply {
                            moveTo(center.x, center.y)
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    center.x - radius, center.y - radius,
                                    center.x + radius, center.y + radius,
                                ),
                                startAngleDegrees = start,
                                sweepAngleDegrees = sweep,
                                forceMoveTo = false,
                            )
                            lineTo(center.x, center.y)
                            close()
                        }
                        drawPath(path, sector.color)
                        drawPath(
                            path,
                            color = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                            style = Stroke(width = 3f),
                        )

                        val midAngleRad = (start + sweep / 2f) * PI.toFloat() / 180f
                        val labelDist = radius * 0.6f
                        val lx = center.x + cos(midAngleRad) * labelDist
                        val ly = center.y + sin(midAngleRad) * labelDist
                        val style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val layout = measurer.measure(sector.label, style)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                lx - layout.size.width / 2f,
                                ly - layout.size.height / 2f,
                            ),
                        )
                    }

                    drawCircle(
                        color = GameColors.SubmitBg,
                        radius = radius * 0.14f,
                        center = center,
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius * 0.14f,
                        center = center,
                        style = Stroke(width = 3f),
                    )
                }

                // Pointer (stays put; only the wheel rotates)
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(40.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w / 2f, h)
                        lineTo(w * 0.18f, 0f)
                        lineTo(w * 0.82f, 0f)
                        close()
                    }
                    drawPath(path, Color(0xFFFFDC50))
                    drawPath(path, Color(0xFF000000).copy(alpha = 0.4f),
                             style = Stroke(width = 3f))
                }
            }

            Spacer(Modifier.height(20.dp))

            // SPIN button (disabled after the first spin)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (hasSpun) Color(0xFF555555) else Color(0xFF32C850)
                    )
                    .clickable(enabled = !hasSpun) {
                        val totalWeight = sectors.sumOf { it.weight }
                        val pick = Random.nextInt(totalWeight)
                        var acc = 0
                        var idx = 0
                        for ((i, sector) in sectors.withIndex()) {
                            acc += sector.weight
                            if (pick < acc) { idx = i; break }
                        }
                        resultIndex = idx
                        hasSpun = true

                        val sweep = 360f / sectors.size
                        // Wheel rotates clockwise; sector i centred at i*sweep
                        // (relative to the pointer at top). 5 full turns + the
                        // negative offset positions sector idx under pointer.
                        rotationTarget = (360f * 5 - idx * sweep) to idx
                    }
                    .padding(horizontal = 36.dp, vertical = 14.dp),
            ) {
                Text(
                    text = if (hasSpun) "TAP TO CLOSE" else "SPIN",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Drive the actual rotation animation, then report the reward
        // back to the caller (which persists it).
        rotationTarget?.let { (target, idx) ->
            LaunchedEffect(target, idx) {
                rotation.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 3200,
                        easing = LinearOutSlowInEasing,
                    ),
                )
                onSpinResult(sectors[idx])
            }
        }
    }
}
