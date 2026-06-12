package com.wheelword.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily
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

/** The default 8-sector wheel, in the v2 palette. Casual-friendly:
 *  small wins are common, rarer 50-coin or 3-hint jackpots keep the
 *  spin exciting. */
val DEFAULT_SECTORS: List<SpinSector> = listOf(
    SpinSector(coins = 5,  hints = 0, color = Color(0xFF4D9BE8), weight = 28),
    SpinSector(coins = 10, hints = 0, color = Color(0xFFFFC23E), weight = 22),
    SpinSector(coins = 15, hints = 0, color = Color(0xFF4D9BE8), weight = 16),
    SpinSector(coins = 0,  hints = 1, color = Color(0xFF34C26B), weight = 12),
    SpinSector(coins = 20, hints = 0, color = Color(0xFFFFC23E), weight = 10),
    SpinSector(coins = 25, hints = 0, color = Color(0xFF4D9BE8), weight = 6),
    SpinSector(coins = 50, hints = 0, color = Color(0xFFE85454), weight = 4),
    SpinSector(coins = 0,  hints = 3, color = Color(0xFF34C26B), weight = 2),
)

/**
 * Daily Spin modal in the Word Wheel v2 design: navy gradient panel,
 * gold pointer, candy-coloured wheel, green SPIN → gold CLAIM.
 * Reward is reported via [onSpinResult]; the caller credits it when
 * the dialog dismisses.
 */
@Composable
fun SpinWheelDialog(
    sectors: List<SpinSector> = DEFAULT_SECTORS,
    alreadySpun: Boolean = false,
    onSpinResult: (SpinSector) -> Unit,
    onDismiss: () -> Unit,
) {
    var hasSpun by rememberSaveable { mutableStateOf(false) }
    // Plain remember (not saveable): if the composition is ever rebuilt
    // mid-spin the animation is gone too, and a persisted `spinning =
    // true` would leave the dialog stuck undismissable.
    var spinning by remember { mutableStateOf(false) }
    var resultIndex by rememberSaveable { mutableIntStateOf(-1) }
    // Pair<finalRotationDegrees, sectorIndex> — set once the user taps
    // SPIN; observed by the LaunchedEffect below.
    var rotationTarget by remember { mutableStateOf<Pair<Float, Int>?>(null) }
    val rotation = remember { Animatable(0f) }

    val measurer = rememberTextMeasurer()

    ModalShell(onDismiss = if (spinning) null else onDismiss) {
        Text(
            text = if (hasSpun && !spinning) "Nice!" else "Daily Spin",
            color = Color.White,
            fontFamily = BalooFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                spinning -> "Good luck…"
                hasSpun -> sectors.getOrNull(resultIndex)?.let { sec ->
                    when {
                        sec.coins > 0 && sec.hints > 0 ->
                            "You won +${sec.coins} coins and +${sec.hints} hints!"
                        sec.coins > 0 -> "You won +${sec.coins} coins!"
                        sec.hints > 0 -> "You won +${sec.hints} hint${if (sec.hints > 1) "s" else ""} 💡!"
                        else -> ""
                    }
                } ?: ""
                alreadySpun -> "Come back tomorrow!"
                else -> "Tap below to spin — once per day"
            },
            color = GameColors.Gold,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

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
                    drawPath(path, color = Color.White, style = Stroke(width = 5f))

                    val midAngleRad = (start + sweep / 2f) * PI.toFloat() / 180f
                    val labelDist = radius * 0.64f
                    val lx = center.x + cos(midAngleRad) * labelDist
                    val ly = center.y + sin(midAngleRad) * labelDist
                    val style = TextStyle(
                        color = Color.White,
                        fontSize = 19.sp,
                        fontFamily = BalooFamily,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
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

                // Hub — sky-blue disc with a white ring.
                drawCircle(color = Color(0xFF5FA9EE), radius = radius * 0.14f, center = center)
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.14f,
                    center = center,
                    style = Stroke(width = 6f),
                )
            }

            // Pointer (stays put; only the wheel rotates) — gold with a
            // deep-gold outline, like the design's drop-shadowed marker.
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(36.dp),
            ) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.1f, h * 0.06f)
                    lineTo(w * 0.9f, h * 0.06f)
                    lineTo(w / 2f, h)
                    close()
                }
                drawPath(path, GameColors.Gold)
                drawPath(path, GameColors.GoldDeep, style = Stroke(width = 5f))
            }
        }

        Spacer(Modifier.height(18.dp))

        if (hasSpun && !spinning) {
            CandyButton(
                text = "CLAIM",
                onClick = onDismiss,
                color = GameColors.Gold,
                colorDeep = GameColors.GoldDeep,
                fontSize = 21.sp,
                modifier = Modifier.fillMaxWidth(0.7f),
                verticalPadding = 12.dp,
            )
        } else {
            val spinDisabled = spinning || alreadySpun
            CandyButton(
                text = if (spinning) "…" else "SPIN",
                onClick = {
                    if (spinDisabled || hasSpun) return@CandyButton
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
                    spinning = true

                    val sweep = 360f / sectors.size
                    // Wheel rotates clockwise; sector i centred at i*sweep
                    // (relative to the pointer at top). 5 full turns + the
                    // negative offset positions sector idx under pointer.
                    rotationTarget = (360f * 5 - idx * sweep) to idx
                },
                color = if (spinDisabled) Color(0xFF555F75) else GameColors.Green,
                colorDeep = if (spinDisabled) Color(0xFF424B5E) else GameColors.GreenDeep,
                fontSize = 22.sp,
                modifier = Modifier.fillMaxWidth(0.6f),
                verticalPadding = 13.dp,
            )
        }

        // Drive the actual rotation animation, then report the reward
        // back to the caller (which persists it).
        rotationTarget?.let { (target, idx) ->
            LaunchedEffect(target, idx) {
                rotation.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = 3600,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.12f, 0.62f, 0.06f, 1f),
                    ),
                )
                spinning = false
                onSpinResult(sectors[idx])
            }
        }
    }
}
