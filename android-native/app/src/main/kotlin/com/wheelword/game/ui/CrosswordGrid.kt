package com.wheelword.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.Level
import com.wheelword.game.theme.GameColors

/**
 * Renders the crossword grid. Cell size is derived from the parent's available
 * width *and* an optional [maxHeight] cap so the grid scales gracefully on
 * everything from a 4.7" phone (cells shrink) up to a 10" tablet (cells cap
 * at [MAX_CELL]).
 */
@Composable
fun CrosswordGrid(
    level: Level,
    visible: Map<Pair<Int, Int>, Char>,
    usedCells: Set<Pair<Int, Int>>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
) {
    val gap = 3.dp
    val padding = 12.dp

    BoxWithConstraints(modifier = modifier) {
        val widthBudget = maxWidth - padding * 2 - gap * (level.cols - 1)
        val widthCell = (widthBudget / level.cols).coerceAtLeast(MIN_CELL)

        val heightLimit = if (maxHeight != Dp.Unspecified) maxHeight else this.maxHeight
        val heightCell = if (heightLimit != Dp.Unspecified && heightLimit.value > 0f) {
            val heightBudget = heightLimit - padding * 2 - gap * (level.rows - 1)
            (heightBudget / level.rows).coerceAtLeast(MIN_CELL)
        } else widthCell

        val cellSize = minOf(widthCell, heightCell, MAX_CELL)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GameColors.GridBackdrop)
                .padding(padding)
                .align(Alignment.Center),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (r in 0 until level.rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (c in 0 until level.cols) {
                            if (usedCells.contains(r to c)) {
                                GridCell(size = cellSize, letter = visible[r to c])
                            } else {
                                Spacer(Modifier.size(cellSize))
                            }
                        }
                    }
                }
            }
        }
    }
}

private val MIN_CELL = 22.dp
private val MAX_CELL = 56.dp

@Composable
private fun GridCell(size: Dp, letter: Char?) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(if (letter != null) GameColors.CellFilled else GameColors.CellEmpty),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Text(
                text = letter.toString(),
                color = GameColors.CellFoundText,
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
