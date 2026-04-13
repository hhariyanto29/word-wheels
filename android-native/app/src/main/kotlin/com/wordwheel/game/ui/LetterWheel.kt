package com.wordwheel.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.wordwheel.game.theme.GameColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun LetterWheel(
    tiles: List<Char>,
    selection: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    onSubmit: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wheelSize by remember { mutableStateOf(Size.Zero) }
    val textMeasurer = rememberTextMeasurer()

    // Track selection as mutable list inside composable so drag can mutate during gesture.
    val currentSelection = remember(selection) { selection.toMutableList() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(tiles) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val positions = tilePositions(wheelSize, tiles.size)
                        val tileR = wheelSize.width * 0.085f
                        val hit = positions.indexOfFirst { distance(it, pos) <= tileR + 10f }
                        if (hit >= 0) {
                            currentSelection.clear()
                            currentSelection.add(hit)
                            onSelectionChange(currentSelection.toList())
                        }
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        val positions = tilePositions(wheelSize, tiles.size)
                        val tileR = wheelSize.width * 0.085f
                        val hit = positions.indexOfFirst { distance(it, pos) <= tileR + 10f }
                        if (hit >= 0 && !currentSelection.contains(hit)) {
                            currentSelection.add(hit)
                            onSelectionChange(currentSelection.toList())
                        }
                    },
                    onDragEnd = {
                        onSubmit()
                    },
                    onDragCancel = {
                        onSubmit()
                    },
                )
            }
            .pointerInput(tiles) {
                detectTapGestures(
                    onTap = { pos ->
                        val center = Offset(wheelSize.width / 2f, wheelSize.height / 2f)
                        val shuffleR = wheelSize.width * 0.09f
                        if (distance(center, pos) <= shuffleR) {
                            onShuffle()
                            return@detectTapGestures
                        }
                        val positions = tilePositions(wheelSize, tiles.size)
                        val tileR = wheelSize.width * 0.085f
                        val hit = positions.indexOfFirst { distance(it, pos) <= tileR + 10f }
                        if (hit >= 0 && !currentSelection.contains(hit)) {
                            currentSelection.add(hit)
                            onSelectionChange(currentSelection.toList())
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            wheelSize = size
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f * 0.95f
            val tileOrbit = radius * 0.6f
            val tileR = radius * 0.17f

            // Wheel background
            drawCircle(
                color = GameColors.WheelBg,
                radius = radius,
                center = center,
            )
            drawCircle(
                color = Color(0x78FFFFFF),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f),
            )

            // Tile positions
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
                    val a = positions[selection[i]]
                    val b = positions[selection[i + 1]]
                    drawLine(
                        color = GameColors.LineColor,
                        start = a,
                        end = b,
                        strokeWidth = 8f,
                    )
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
                    tileR = tileR,
                    color = if (isSelected) Color.White else GameColors.LetterColor,
                )
            }

            // Shuffle icon in center
            drawTileLetter(
                measurer = textMeasurer,
                letter = "\u21C4",
                center = center,
                tileR = radius * 0.13f,
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
    if (size.width == 0f) return emptyList()
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTileLetter(
    measurer: TextMeasurer,
    letter: String,
    center: Offset,
    tileR: Float,
    color: Color,
) {
    val style = TextStyle(
        color = color,
        fontSize = (tileR * 1.1f / density).sp,
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
