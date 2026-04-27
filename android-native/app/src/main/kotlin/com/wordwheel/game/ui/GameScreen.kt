package com.wordwheel.game.ui

import androidx.compose.foundation.Image
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
import com.wordwheel.game.LocalGameStorage
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
    val sound = LocalSoundManager.current
    val storage = LocalGameStorage.current

    // Load the saved snapshot (if any) and build a single long-lived
    // GameState. Level transitions mutate it in place via goToLevel()
    // rather than re-creating the object, so the persist callback keeps
    // its closure valid for the app's lifetime.
    //
    // The `holder` array is a forward-reference so the persist lambda
    // can call back into the GameState we're still constructing.
    val game = remember {
        val holder = arrayOfNulls<GameState>(1)
        val saved = storage?.load()
        val state = GameState(
            levelNum = saved?.levelNum ?: 1,
            initialCoins = saved?.coins ?: 200,
            initialHints = saved?.hintsLeft ?: 5,
            initialWordsTowardHint = saved?.wordsTowardHint ?: 0,
            persist = { holder[0]?.let { s -> storage?.save(s.snapshot()) } },
        )
        holder[0] = state
        if (saved != null) state.restore(saved)
        state
    }

    // Tick the streak once per app open. Uses the device's local epoch
    // day so the rollover happens at the player's local midnight.
    LaunchedEffect(Unit) {
        val today = java.time.LocalDate.now().toEpochDay()
        game.tickDailyStreak(today)
    }
    val today = remember { java.time.LocalDate.now().toEpochDay() }

    var status by remember { mutableStateOf("") }
    var spinDialogOpen by remember { mutableStateOf(false) }
    var pendingDifficultyTier by remember { mutableStateOf<com.wordwheel.game.Difficulty?>(null) }
    var pendingNextLevel by remember { mutableStateOf<Int?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    // App opens on the home screen; tap "LEVEL X" to enter the puzzle.
    var atHome by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(game.levelNum, game.isComplete()) {
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
        game.goToLevel(newLevel)
        status = ""
    }

    // Home screen — pre-game landing with the resume button. Tapping
    // LEVEL X switches `atHome` off and the puzzle takes over.
    if (atHome) {
        HomeScreen(
            levelNum = game.levelNum,
            coins = game.coins,
            streak = game.currentStreak,
            spinAvailable = game.canSpinToday(today),
            onResume = { atHome = false },
            onSpinClick = { spinDialogOpen = true },
            onSettingsClick = { settingsOpen = true },
        )
        // Even on the home screen, the spin and settings dialogs may be
        // open (player tapped from the home buttons).
        if (spinDialogOpen) {
            SpinWheelDialog(
                onSpinResult = { sector ->
                    game.applySpinReward(
                        today = today,
                        coinsAdded = sector.coins,
                        hintsAdded = sector.hints,
                    )
                    if (sector.coins > 0) sound?.play(Sfx.WordFound)
                    else if (sector.hints > 0) sound?.play(Sfx.Hint)
                },
                onDismiss = { spinDialogOpen = false },
            )
        }
        if (settingsOpen) {
            SettingsDialog(
                soundManager = sound,
                onDismiss = { settingsOpen = false },
            )
        }
        return
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
                level = game.levelNum,
                streak = game.currentStreak,
                spinAvailable = game.canSpinToday(today),
                status = status,
                spec = spec,
                onSubmitWheel = {
                    if (game.currentWord().length >= 2) {
                        status = game.trySubmit(); playForStatus(status)
                    } else game.clearSelection()
                },
                onShuffle = { game.shuffleTiles() },
                onHint = { status = game.hintRevealRandomLetter(); playForStatus(status) },
                onSpinClick = { spinDialogOpen = true },
                onSettingsClick = { settingsOpen = true },
            )
        } else {
            PortraitContent(
                game = game,
                level = game.levelNum,
                streak = game.currentStreak,
                spinAvailable = game.canSpinToday(today),
                status = status,
                spec = spec,
                onSubmitWheel = {
                    if (game.currentWord().length >= 2) {
                        status = game.trySubmit(); playForStatus(status)
                    } else game.clearSelection()
                },
                onShuffle = { game.shuffleTiles() },
                onHint = { status = game.hintRevealRandomLetter(); playForStatus(status) },
                onSpinClick = { spinDialogOpen = true },
                onSettingsClick = { settingsOpen = true },
            )
        }

        if (spinDialogOpen) {
            SpinWheelDialog(
                onSpinResult = { sector ->
                    game.applySpinReward(
                        today = today,
                        coinsAdded = sector.coins,
                        hintsAdded = sector.hints,
                    )
                    if (sector.coins > 0) sound?.play(Sfx.WordFound)
                    else if (sector.hints > 0) sound?.play(Sfx.Hint)
                },
                onDismiss = { spinDialogOpen = false },
            )
        }

        if (game.isComplete() && pendingDifficultyTier == null) {
            CompletionDialog(
                isLastLevel = game.levelNum >= Level.TOTAL_LEVELS,
                onNext = {
                    val completed = game.levelNum
                    game.coins += 10
                    val next = if (completed >= Level.TOTAL_LEVELS) 1 else completed + 1
                    // If this was a tier-boundary level, show the difficulty
                    // banner first; the player advances after dismissing it.
                    val tier = com.wordwheel.game.Difficulty.milestoneFor(completed)
                    if (tier != null) {
                        pendingDifficultyTier = tier
                        // Stage the level transition until the banner closes.
                        pendingNextLevel = next
                    } else {
                        goToLevel(next)
                    }
                },
            )
        }

        pendingDifficultyTier?.let { tier ->
            DifficultyDialog(
                tier = tier,
                onDismiss = {
                    val next = pendingNextLevel
                    pendingDifficultyTier = null
                    pendingNextLevel = null
                    if (next != null) goToLevel(next)
                },
            )
        }

        if (settingsOpen) {
            SettingsDialog(
                soundManager = sound,
                onDismiss = { settingsOpen = false },
            )
        }
    }
}

/* ---------- Portrait ---------- */

@Composable
private fun PortraitContent(
    game: GameState,
    level: Int,
    streak: Int,
    spinAvailable: Boolean,
    status: String,
    spec: Spec,
    onSubmitWheel: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    onSpinClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TopBar(
                    coins = game.coins,
                    found = game.found.size,
                    total = game.answers.size,
                    level = level,
                    streak = streak,
                )
            }
            if (spinAvailable) {
                Spacer(Modifier.width(8.dp))
                SpinPill(onClick = onSpinClick)
            }
            Spacer(Modifier.width(8.dp))
            SettingsIconButton(onClick = onSettingsClick)
        }
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
    streak: Int,
    spinAvailable: Boolean,
    status: String,
    spec: Spec,
    onSubmitWheel: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    onSpinClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TopBar(
                    coins = game.coins,
                    found = game.found.size,
                    total = game.answers.size,
                    level = level,
                    streak = streak,
                )
            }
            if (spinAvailable) {
                Spacer(Modifier.width(8.dp))
                SpinPill(onClick = onSpinClick)
            }
            Spacer(Modifier.width(8.dp))
            SettingsIconButton(onClick = onSettingsClick)
        }
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

@Composable
private fun SpinPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(GameColors.GemGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "🎁 SPIN",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x40FFFFFF))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Text(
            text = "⚙",
            color = Color.White,
            fontSize = 22.sp,
        )
    }
}
