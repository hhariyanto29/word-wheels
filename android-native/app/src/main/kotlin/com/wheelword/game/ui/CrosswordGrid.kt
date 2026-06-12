package com.wheelword.game.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.Level
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors

/**
 * Renders the crossword grid in the Word Wheel v2 style: a frosted
 * navy board panel, near-white empty tiles, and blue-gradient filled
 * tiles that pop in when revealed. Cell size is derived from the
 * parent's available width *and* an optional [maxHeight] cap so the
 * grid scales gracefully from small phones to tablets.
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
    val padding = 10.dp

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
                .clip(RoundedCornerShape(22.dp))
                .background(GameColors.BoardPanel)
                .border(width = 1.dp, color = GameColors.StrokeSoft, shape = RoundedCornerShape(22.dp))
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
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        // Base (empty) tile — always present so the filled tile pops
        // in over it, like the prototype's layered cells.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.21f))
                .background(GameColors.TileEmpty),
        )
        if (letter != null) {
            // Scale-in pop on reveal (wwCellPop): grows from 30% with a
            // slight overshoot. `appeared` flips after the first frame so
            // animateFloatAsState actually animates instead of starting
            // at the target.
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            val pop by animateFloatAsState(
                targetValue = if (appeared) 1f else 0.3f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "cellPop",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pop)
                    .clip(RoundedCornerShape(size * 0.21f))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GameColors.TileFillA, GameColors.TileFillB),
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x40FFFFFF),
                            0.25f to Color.Transparent,
                            0.85f to Color.Transparent,
                            1f to Color(0x33000000),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    color = Color.White,
                    fontFamily = BalooFamily,
                    fontSize = (size.value * 0.52f).sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
