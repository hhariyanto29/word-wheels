package com.wordwheel.game.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordwheel.game.GameState
import com.wordwheel.game.Level
import com.wordwheel.game.R
import com.wordwheel.game.audio.LocalSoundManager
import com.wordwheel.game.audio.Sfx
import com.wordwheel.game.theme.GameColors

/**
 * Spacing & sizing tunables derived from the available viewport. Centralising
 * them in one place keeps the per-orientation branches readable instead of
 * scattering `if (compact) Xdp else Ydp` everywhere.
 */
private data class Spec(
    val outerH: Dp,
    val outerV: Dp,
    val gapAfterTopBar: Dp,
    val gapAfterGrid: Dp,
    val gapAfterWord: Dp,
    val gapBeforeButtons: Dp,
    val statusFontSp: Int,
)

private fun specFor(width: Dp, height: Dp, landscape: Boolean): Spec {
    val short = minOf(width, height)
    val compact = height < 680.dp || (landscape && height < 420.dp)
    return Spec(
        outerH = if (short < 360.dp) 10.dp else 16.dp,
        outerV = if (compact) 12.dp else 24.dp,
        gapAfterTopBar = if (compact) 8.dp else 16.dp,
        gapAfterGrid = if (compact) 8.dp else 14.dp,
        gapAfterWord = if (compact) 4.dp else 6.dp,
        gapBeforeButtons = if (compact) 4.dp else 8.dp,
        statusFontSp = if (short < 360.dp) 13 else 15,
    )
}

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
    val sound = LocalSoundManager.current

    LaunchedEffect(currentLevel, gameKey, game.isComplete()) {
        if (game.isComplete()) sound?.play(Sfx.Complete)
    }

    val playForStatus: (String) -> Unit = { msg ->
        when {
            msg.startsWith("Found:") -> sound?.play(Sfx.WordFound)
            msg.startsWith("Bonus:") -> sound?.play(Sfx.Bonus)
            msg.startsWith("No match") -> sound?.play(Sfx.Wrong)
            msg.startsWith("Revealed") -> sound?.play(Sfx.Hint)
            else -> Unit
        }
    }

    val goToLevel: (Int) -> Unit = { newLevel ->
        carriedCoins = game.coins
        carriedHints = game.hintsLeft
        carriedWordsTowardHint = game.wordsTowardHint
        currentLevel = newLevel
        gameKey += 1
        status = ""
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Desert-toned gradient visible behind the image (shows at the
                // edges on wide aspect ratios where Crop letterboxes).
                Brush.verticalGradient(
                    colors = listOf(GameColors.BgTop, GameColors.BgBottom)
                )
            ),
    ) {
        val w = maxWidth
        val h = maxHeight
        val landscape = w > h
        val spec = specFor(w, h, landscape)

        // HOPEWELL WHELD WORD background. ContentScale.Crop keeps the
        // pyramid/desert artwork full-bleed; the semi-transparent overlay
        // keeps the white text legible over the bright sky.
        Image(
            painter = painterResource(R.drawable.game_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66000014),
                            Color(0x4D000028),
                            Color(0x80000032),
                        )
                    )
                ),
        )

        if (landscape) {
            LandscapeContent(
                game = game,
                level = currentLevel,
                status = status,
                spec = spec,
                onSubmitWheel = {
                    if (game.currentWord().length >= 2) {
                        status = game.trySubmit(); playForStatus(status)
                    } else game.clearSelection()
                },
                onShuffle = { game.shuffleTiles() },
                onHint = { status = game.hintRevealRandomLetter(); playForStatus(status) },
                onSubmitButton = { status = game.trySubmit(); playForStatus(status) },
                onBackspace = {
                    if (game.selection.isNotEmpty()) {
                        sound?.play(Sfx.Backspace); game.backspace()
                    }
                },
            )
        } else {
            PortraitContent(
                game = game,
                level = currentLevel,
                status = status,
                spec = spec,
                onSubmitWheel = {
                    if (game.currentWord().length >= 2) {
                        status = game.trySubmit(); playForStatus(status)
                    } else game.clearSelection()
                },
                onShuffle = { game.shuffleTiles() },
                onHint = { status = game.hintRevealRandomLetter(); playForStatus(status) },
                onSubmitButton = { status = game.trySubmit(); playForStatus(status) },
                onBackspace = {
                    if (game.selection.isNotEmpty()) {
                        sound?.play(Sfx.Backspace); game.backspace()
                    }
                },
            )
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

/* ---------- Portrait ---------- */

@Composable
private fun PortraitContent(
    game: GameState,
    level: Int,
    status: String,
    spec: Spec,
    onSubmitWheel: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    onSubmitButton: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(
            coins = game.coins,
            found = game.found.size,
            total = game.answers.size,
            level = level,
        )
        Spacer(Modifier.height(spec.gapAfterTopBar))

        CrosswordGrid(
            level = game.level,
            visible = game.visibleLettersMap(),
            usedCells = game.usedCells,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(spec.gapAfterGrid))

        WordPreview(text = game.currentWord())
        Spacer(Modifier.height(spec.gapAfterWord))

        StatusBubble(status = status, fontSp = spec.statusFontSp)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val wheelSize = minOf(this.maxWidth, this.maxHeight)
            LetterWheel(
                tiles = game.tiles,
                selection = game.selection,
                onSubmit = onSubmitWheel,
                onShuffle = onShuffle,
                modifier = Modifier.size(wheelSize),
            )
        }

        Spacer(Modifier.height(spec.gapBeforeButtons))
        BottomButtons(
            hintsLeft = game.hintsLeft,
            wordsTowardHint = game.wordsTowardHint,
            onHint = onHint,
            onSubmit = onSubmitButton,
            onBackspace = onBackspace,
        )

        if (game.bonusFound.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Bonus: ${game.bonusFound.joinToString(", ")}",
                color = Color(0xA0FFFFFF),
                fontSize = 12.sp,
            )
        }
    }
}

/* ---------- Landscape ---------- */

@Composable
private fun LandscapeContent(
    game: GameState,
    level: Int,
    status: String,
    spec: Spec,
    onSubmitWheel: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    onSubmitButton: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
    ) {
        TopBar(
            coins = game.coins,
            found = game.found.size,
            total = game.answers.size,
            level = level,
        )
        Spacer(Modifier.height(spec.gapAfterTopBar))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: grid + status
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CrosswordGrid(
                    level = game.level,
                    visible = game.visibleLettersMap(),
                    usedCells = game.usedCells,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(spec.gapAfterGrid))
                WordPreview(text = game.currentWord())
                Spacer(Modifier.height(spec.gapAfterWord))
                StatusBubble(status = status, fontSp = spec.statusFontSp)
            }

            Spacer(Modifier.width(16.dp))

            // Right: wheel + buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val wheelSize = minOf(this.maxWidth, this.maxHeight)
                    LetterWheel(
                        tiles = game.tiles,
                        selection = game.selection,
                        onSubmit = onSubmitWheel,
                        onShuffle = onShuffle,
                        modifier = Modifier.size(wheelSize),
                    )
                }
                Spacer(Modifier.height(spec.gapBeforeButtons))
                BottomButtons(
                    hintsLeft = game.hintsLeft,
                    wordsTowardHint = game.wordsTowardHint,
                    onHint = onHint,
                    onSubmit = onSubmitButton,
                    onBackspace = onBackspace,
                )
                if (game.bonusFound.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bonus: ${game.bonusFound.joinToString(", ")}",
                        color = Color(0xA0FFFFFF),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/* ---------- Small reusable bits ---------- */

@Composable
private fun WordPreview(text: String) {
    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(GameColors.WheelBg)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = text,
                color = GameColors.LetterColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun StatusBubble(status: String, fontSp: Int) {
    if (status.isNotEmpty()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x8C000000))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = status,
                color = Color.White,
                fontSize = fontSp.sp,
            )
        }
    } else {
        Spacer(Modifier.height(30.dp))
    }
}
