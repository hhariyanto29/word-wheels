package com.wordwheel.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.GameState
import com.wordwheel.game.Level
import com.wordwheel.game.theme.GameColors

@Composable
fun GameScreen() {
    var currentLevel by rememberSaveable { mutableIntStateOf(1) }
    var gameKey by rememberSaveable { mutableIntStateOf(0) }
    var carriedCoins by rememberSaveable { mutableIntStateOf(200) }
    var carriedHints by rememberSaveable { mutableIntStateOf(5) }
    var carriedWordsTowardHint by rememberSaveable { mutableIntStateOf(0) }

    val game = remember(currentLevel, gameKey) {
        GameState(currentLevel, carriedCoins).apply {
            hintsLeft = carriedHints
            wordsTowardHint = carriedWordsTowardHint
        }
    }

    var status by remember { mutableStateOf("") }

    val goToLevel: (Int) -> Unit = { newLevel ->
        carriedCoins = game.coins
        carriedHints = game.hintsLeft
        carriedWordsTowardHint = game.wordsTowardHint
        currentLevel = newLevel
        gameKey += 1
        status = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GameColors.BgTop, GameColors.BgBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                coins = game.coins,
                found = game.found.size,
                total = game.answers.size,
                level = currentLevel,
            )

            Spacer(Modifier.height(16.dp))

            CrosswordGrid(
                level = game.level,
                visible = game.visibleLettersMap(),
                usedCells = game.usedCells,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Spacer(Modifier.height(14.dp))

            val currentWord = game.currentWord()
            if (currentWord.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(GameColors.WheelBg)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentWord,
                        color = GameColors.LetterColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(6.dp))
            } else {
                Spacer(Modifier.height(36.dp))
            }

            if (status.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Color(0x8C000000))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = status,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 15.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(30.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LetterWheel(
                    tiles = game.tiles,
                    selection = game.selection,
                    onSubmit = {
                        if (game.currentWord().length >= 2) {
                            status = game.trySubmit()
                        } else {
                            game.clearSelection()
                        }
                    },
                    onShuffle = { game.shuffleTiles() },
                )
            }

            BottomButtons(
                hintsLeft = game.hintsLeft,
                wordsTowardHint = game.wordsTowardHint,
                onHint = { status = game.hintRevealRandomLetter() },
                onSubmit = { status = game.trySubmit() },
                onBackspace = { game.backspace() },
            )

            if (game.bonusFound.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Bonus: ${game.bonusFound.joinToString(", ")}",
                    color = androidx.compose.ui.graphics.Color(0xA0FFFFFF),
                    fontSize = 12.sp,
                )
            }
        }

        if (game.isComplete()) {
            CompletionDialog(
                isLastLevel = currentLevel >= Level.TOTAL_LEVELS,
                onNext = {
                    game.coins += 10
                    val next = if (currentLevel >= Level.TOTAL_LEVELS) 1 else currentLevel + 1
                    goToLevel(next)
                },
            )
        }
    }
}
