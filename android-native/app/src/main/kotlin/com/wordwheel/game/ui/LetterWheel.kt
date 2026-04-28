package com.wordwheel.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.toSize
import com.wordwheel.game.audio.LocalSoundManager
import com.wordwheel.game.audio.Sfx
import com.wordwheel.game.theme.GameColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders the letter wheel. [selection] is passed as a [SnapshotStateList] so
 * gesture callbacks mutate it directly — avoiding stale-closure bugs that
 * would occur with an immutable [List] + callback pattern (the pointerInput
 * block is only re-launched when [tiles] changes, so any local mutable copy
 * would go stale the moment the parent clears the selection).
 */
@Composable
fun LetterWheel(
    tiles: List<Char>,
    selection: SnapshotStateList<Int>,
    onSubmit: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wheelSize by remember { mutableStateOf(Size.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val sound = LocalSoundManager.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { wheelSize = it.toSize() }
            .pointerInput(tiles) {
                // `lastPos` tracks the previous drag sample so we can
                // interpolate a fast slide. Without interpolation a quick
                // swipe across the wheel skips tiles whose centers fall
                // between consecutive drag events.
                var lastPos: Offset? = null
                detectDragGestures(
                    onDragStart = { pos ->
                        val hit = hitTest(wheelSize, tiles.size, pos)
                        if (hit >= 0) {
                            selection.clear()
                            selection.add(hit)
                            sound?.play(Sfx.Select)
                        }
                        lastPos = pos
                    },
                    onDrag = { change, _ ->
                        val from = lastPos ?: change.position
                        val to = change.position
                        // Sample several points along the segment so a
                        // fast swipe still registers each tile it crosses.
                        // Half a tile-radius per step is small enough that
                        // we can't skip a tile, big enough to keep cost low.
                        val tileRpx = wheelSize.width.coerceAtLeast(1f) *
                            0.5f * 0.95f * 0.17f
                        val stepLen = tileRpx.coerceAtLeast(1f) * 0.5f
                        val steps = (distance(from, to) / stepLen)
                            .toInt()
                            .coerceIn(1, 12)
                        for (s in 1..steps) {
                            val t = s.toFloat() / steps
                            val p = Offset(
                                from.x + (to.x - from.x) * t,
                                from.y + (to.y - from.y) * t,
                            )
                            val hit = hitTest(wheelSize, tiles.size, p)
                            if (hit < 0 || hit == selection.lastOrNull()) continue
                            // Backtrack: dragging back over the previous
                            // tile shortens the word — feels natural and
                            // matches Wordscapes / Words of Wonders.
                            if (selection.size >= 2 &&
                                hit == selection[selection.size - 2]) {
                                selection.removeAt(selection.size - 1)
                                sound?.play(Sfx.Select)
                            } else if (!selection.contains(hit)) {
                                selection.add(hit)
                                sound?.play(Sfx.Select)
                            }
                        }
                        lastPos = to
                    },
                    onDragEnd = { lastPos = null; onSubmit() },
                    onDragCancel = { lastPos = null; onSubmit() },
                )
            }
            .pointerInput(tiles) {
                detectTapGestures(
                    onTap = { pos ->
                        // Center tap → shuffle
                        val center = Offset(wheelSize.width / 2f, wheelSize.height / 2f)
                        val shuffleR = wheelSize.width * 0.09f
                        if (distance(center, pos) <= shuffleR) {
                            sound?.play(Sfx.Shuffle)
                            onShuffle()
                            return@detectTapGestures
                        }
                        val hit = hitTest(wheelSize, tiles.size, pos)
                        if (hit >= 0 && !selection.contains(hit)) {
                            selection.add(hit)
                            sound?.play(Sfx.Select)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f * 0.95f
            val tileOrbit = radius * 0.6f
            val tileR = radius * 0.17f

            drawCircle(color = GameColors.WheelBg, radius = radius, center = center)
            drawCircle(
                color = Color(0x78FFFFFF),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f),
            )

            val positions = List(tiles.size) { i ->
                val angle = i.toFloat() / tiles.size * 2f * PI.toFloat() - PI.toFloat() / 2f
                Offset(
                    center.x + cos(angle) * tileOrbit,
                    center.y + sin(angle) * tileOrbit,
                )
            }

            // Selection lines
            if (selection.size >= 2) {
                for (i in 0 until selection.size - 1) {
                    val ai = selection[i]
                    val bi = selection[i + 1]
                    if (ai in positions.indices && bi in positions.indices) {
                        drawLine(
                            color = GameColors.LineColor,
                            start = positions[ai],
                            end = positions[bi],
                            strokeWidth = 8f,
                        )
                    }
                }
            }

            // Tiles
            for (i in tiles.indices) {
                val pos = positions[i]
                val isSelected = selection.contains(i)
                if (isSelected) {
                    drawCircle(
                        color = GameColors.TileSelectedBg,
                        radius = tileR,
                        center = pos,
                    )
                }
                drawTileLetter(
                    measurer = textMeasurer,
                    letter = tiles[i].toString(),
                    center = pos,
                    sizePx = tileR * 1.1f,
                    color = if (isSelected) Color.White else GameColors.LetterColor,
                )
            }

            // Shuffle icon
            drawTileLetter(
                measurer = textMeasurer,
                letter = "\u21C4",
                center = center,
                sizePx = radius * 0.26f,
                color = GameColors.ShuffleIcon,
            )
        }
    }
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun tilePositions(size: Size, n: Int): List<Offset> {
    if (size.width == 0f || n == 0) return emptyList()
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) / 2f * 0.95f
    val tileOrbit = radius * 0.6f
    return List(n) { i ->
        val angle = i.toFloat() / n * 2f * PI.toFloat() - PI.toFloat() / 2f
        Offset(
            center.x + cos(angle) * tileOrbit,
            center.y + sin(angle) * tileOrbit,
        )
    }
}

private fun hitTest(size: Size, n: Int, pos: Offset): Int {
    if (size.width == 0f) return -1
    val radius = minOf(size.width, size.height) / 2f * 0.95f
    val tileR = radius * 0.17f
    val positions = tilePositions(size, n)
    // Generous hit padding (~40% beyond the visible tile) keeps drags
    // forgiving when fingers slide quickly — the previous +12px constant
    // was tight enough that fast swipes regularly missed tiles.
    val hitR = tileR * 1.45f
    return positions.indexOfFirst { distance(it, pos) <= hitR }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTileLetter(
    measurer: TextMeasurer,
    letter: String,
    center: Offset,
    sizePx: Float,
    color: Color,
) {
    val style = TextStyle(
        color = color,
        fontSize = sizePx.toSp(),
        fontWeight = FontWeight.Bold,
    )
    val layout = measurer.measure(text = letter, style = style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f,
        ),
    )
}
