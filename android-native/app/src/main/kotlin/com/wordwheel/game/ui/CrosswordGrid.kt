package com.wordwheel.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.Level
import com.wordwheel.game.theme.GameColors

@Composable
fun CrosswordGrid(
    level: Level,
    visible: Map<Pair<Int, Int>, Char>,
    usedCells: Set<Pair<Int, Int>>,
    modifier: Modifier = Modifier,
) {
    val cellSize = when {
        level.cols <= 5 -> 44.dp
        level.cols <= 7 -> 38.dp
        else -> 34.dp
    }
    val gap = 3.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GameColors.GridBackdrop)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0 until level.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until level.cols) {
                        if (usedCells.contains(r to c)) {
                            val ch = visible[r to c]
                            GridCell(size = cellSize, letter = ch)
                        } else {
                            Spacer(Modifier.size(cellSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(size: androidx.compose.ui.unit.Dp, letter: Char?) {
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
