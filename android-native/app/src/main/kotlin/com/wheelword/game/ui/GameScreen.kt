package com.wheelword.game.ui

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.GameState
import com.wheelword.game.Level
import com.wheelword.game.LocalGameStorage
import com.wheelword.game.audio.LocalSoundManager
import com.wheelword.game.audio.Sfx
import com.wheelword.game.theme.GameColors

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
        outerH = if (short < 360.dp) 8.dp else 12.dp,
        outerV = if (compact) 10.dp else 18.dp,
        gapAfterTopBar = if (compact) 8.dp else 14.dp,
        gapAfterGrid = if (compact) 6.dp else 10.dp,
        gapAfterWord = if (compact) 2.dp else 4.dp,
        gapBeforeButtons = if (compact) 4.dp else 6.dp,
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
    // Spin reward is captured when the wheel stops, but not credited
    // to the player's coin count until the dialog dismisses — that
    // way the TopBar's count-up animation plays visibly afterwards.
    var pendingSpinReward by remember { mutableStateOf<SpinSector?>(null) }
    var pendingDifficultyTier by remember { mutableStateOf<com.wheelword.game.Difficulty?>(null) }
    var pendingNextLevel by remember { mutableStateOf<Int?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    // Help dialog: open by default for first-time players (storage.seenHelp
    // == false). Once the player dismisses, we flip the flag persistently
    // so it doesn't pop again on subsequent launches.
    var helpOpen by remember { mutableStateOf(storage?.seenHelp == false) }
    // App opens on the home screen; tap "LEVEL X" to enter the puzzle.
    var atHome by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(game.levelNum, game.isComplete()) {
        if (game.isComplete()) sound?.play(Sfx.Complete)
    }

    // Auto-clear status after a moment so the bubble doesn't linger.
    // Now that the bubble has no reserved slot, it pops in when set
    // and pops out when cleared — the column reflows around it.
    LaunchedEffect(status) {
        if (status.isNotEmpty()) {
            kotlinx.coroutines.delay(2500)
            status = ""
        }
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
                // Stash the result; we don't credit the player's account
                // until the dialog dismisses. That way the TopBar's
                // count-up + pulse animation actually plays visibly,
                // because before dismiss the dialog covers the bar.
                onSpinResult = { sector -> pendingSpinReward = sector },
                onDismiss = {
                    pendingSpinReward?.let { sec ->
                        game.applySpinReward(
                            today = today,
                            coinsAdded = sec.coins,
                            hintsAdded = sec.hints,
                        )
                        if (sec.coins > 0) sound?.play(Sfx.WordFound)
                        else if (sec.hints > 0) sound?.play(Sfx.Hint)
                    }
                    pendingSpinReward = null
                    spinDialogOpen = false
                },
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

        // Per-level country background: Vietnam, Brunei, Malaysia,
        // Myanmar, Papua Nugini, Filipina, Singapore, Thailand,
        // Indonesia, Bali — one image per 10-level range.
        // ContentScale.Crop keeps the artwork full-bleed; the overlay
        // below keeps text legible over bright skies.
        GameBackgroundImage(level = game.levelNum)
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

        // Inner content layer — respects system bars (status bar at the
        // top, gesture/3-button nav at the bottom). The background image
        // and dark gradient above intentionally render edge-to-edge, but
        // anything tappable lives inside this insets-aware Box so it
        // doesn't slide under the navigation bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            if (landscape) {
                LandscapeContent(
                    game = game,
                    level = game.levelNum,
                    streak = game.currentStreak,
                    status = status,
                    spec = spec,
                    onSubmitWheel = {
                        if (game.currentWord().length >= 2) {
                            status = game.trySubmit(); playForStatus(status)
                        } else game.clearSelection()
                    },
                    onShuffle = { game.shuffleTiles() },
                    onHint = { status = game.hintRevealRandomLetter(); playForStatus(status) },
                    onSettingsClick = { settingsOpen = true },
                )
            } else {
                PortraitContent(
                    game = game,
                    level = game.levelNum,
                    streak = game.currentStreak,
                    status = status,
                    spec = spec,
                    onSubmitWheel = {
                        if (game.currentWord().length >= 2) {
                            status = game.trySubmit(); playForStatus(status)
                        } else game.clearSelection()
                    },
                    onShuffle = { game.shuffleTiles() },
                    onHint = { status = game.hintRevealRandomLetter(); playForStatus(status) },
                    onSettingsClick = { settingsOpen = true },
                )
            }

            // Floating SPIN button. Only shown on the days the daily spin
            // is available — keeps it out of the TopBar row entirely so
            // long coin/word/level labels never get squished by it.
            if (game.canSpinToday(today)) {
                FloatingSpinButton(
                    onClick = { spinDialogOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp),
                )
            }

            // Floating help button — bottom-left, smaller than the SPIN
            // button. Pulled out of the TopBar so the TopBar's level/words
            // labels have room to breathe.
            FloatingHelpButton(
                onClick = { helpOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 26.dp),
            )
        }

        if (spinDialogOpen) {
            SpinWheelDialog(
                // Stash the result; we don't credit the player's account
                // until the dialog dismisses. That way the TopBar's
                // count-up + pulse animation actually plays visibly,
                // because before dismiss the dialog covers the bar.
                onSpinResult = { sector -> pendingSpinReward = sector },
                onDismiss = {
                    pendingSpinReward?.let { sec ->
                        game.applySpinReward(
                            today = today,
                            coinsAdded = sec.coins,
                            hintsAdded = sec.hints,
                        )
                        if (sec.coins > 0) sound?.play(Sfx.WordFound)
                        else if (sec.hints > 0) sound?.play(Sfx.Hint)
                    }
                    pendingSpinReward = null
                    spinDialogOpen = false
                },
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
                    val tier = com.wheelword.game.Difficulty.milestoneFor(completed)
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

        if (helpOpen) {
            HelpDialog(
                onDismiss = {
                    helpOpen = false
                    storage?.seenHelp = true
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
    streak: Int,
    status: String,
    spec: Spec,
    onSubmitWheel: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
    ) {
        // Wheel size is locked to a screen-width fraction — *not* derived
        // from "leftover" Column space — so it never shrinks when the
        // grid grows for harder levels. The minOf vs height keeps it
        // sane on landscape-ish or short windows; the coerceIn ensures
        // a usable floor on tiny phones and a sensible ceiling on tablets.
        val wheelSize = minOf(maxWidth * 0.92f, maxHeight * 0.42f)
            .coerceIn(280.dp, 480.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
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
                // Help moved to a floating button bottom-left (see below).
                // SPIN is also a floating button. Keeping the TopBar row
                // to {labels + settings} only stops the labels from getting
                // squeezed into vertical glyph stacks.
                Spacer(Modifier.width(8.dp))
                SettingsIconButton(onClick = onSettingsClick)
            }
            Spacer(Modifier.height(spec.gapAfterTopBar))

            // Grid takes the leftover vertical space ABOVE the wheel.
            // Capped maxHeight removed — the wheel is now fixed, so the
            // grid no longer competes for it.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CrosswordGrid(
                    level = game.level,
                    visible = game.visibleLettersMap(),
                    usedCells = game.usedCells,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }

            WordPreview(text = game.currentWord())
            StatusBubble(status = status, fontSp = spec.statusFontSp)

            LetterWheel(
                tiles = game.tiles,
                selection = game.selection,
                onSubmit = onSubmitWheel,
                onShuffle = onShuffle,
                modifier = Modifier.size(wheelSize),
            )

            // Word history sits BELOW the wheel as a compact strip — out
            // of the wheel's way visually, easy to glance at after a
            // submit, doesn't reserve screen real estate above the wheel.
            RecentAttemptsRow(attempts = game.recentAttempts)

            BottomButtons(
                hintsLeft = game.hintsLeft,
                wordsTowardHint = game.wordsTowardHint,
                onHint = onHint,
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
    status: String,
    spec: Spec,
    onSubmitWheel: () -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
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
            // Help + SPIN moved to floating buttons — see PortraitContent.
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

            // Right: wheel + buttons + history
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
                // History strip lives directly under the wheel (matches
                // PortraitContent), so the player's eye stays in the
                // same area where they just submitted.
                RecentAttemptsRow(attempts = game.recentAttempts)
                BottomButtons(
                    hintsLeft = game.hintsLeft,
                    wordsTowardHint = game.wordsTowardHint,
                    onHint = onHint,
                )
            }
        }
    }
}

/* ---------- Small reusable bits ---------- */

// Fixed-height wrappers — the wheel size below is computed from the
// remaining vertical space, so any slot whose height varies frame-to-frame
// would resize the wheel as the player types or as a status message
// appears. Reserving a constant height per slot keeps the wheel rock-solid.
private val WordPreviewSlotHeight = 36.dp
private val StatusSlotHeight = 26.dp
// Compact strip — half the original 32dp. Sitting under the wheel,
// not above it, so the player's eye stays where they last tapped.
private val RecentAttemptsSlotHeight = 24.dp
private const val RECENT_ATTEMPTS_SHOWN = 3

@Composable
private fun WordPreview(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WordPreviewSlotHeight),
        contentAlignment = Alignment.Center,
    ) {
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
        }
    }
}

@Composable
private fun StatusBubble(status: String, fontSp: Int) {
    // No reserved slot any more — when status is empty, returns nothing
    // and the column collapses. When set, renders a small dark pill
    // that visually separates from the wheel by its own padding rather
    // than a fixed-height container that could read as overlap.
    if (status.isEmpty()) return
    Box(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = status,
            color = Color.White,
            fontSize = fontSp.sp,
        )
    }
}

@Composable
private fun RecentAttemptsRow(attempts: List<com.wheelword.game.Attempt>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RecentAttemptsSlotHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (attempts.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (a in attempts.take(RECENT_ATTEMPTS_SHOWN)) {
                    AttemptChip(a)
                }
            }
        }
    }
}

@Composable
private fun AttemptChip(attempt: com.wheelword.game.Attempt) {
    val (bg, fg, decoration) = when (attempt.result) {
        com.wheelword.game.AttemptResult.GRID -> Triple(
            GameColors.GemGreen,
            Color.White,
            null,
        )
        com.wheelword.game.AttemptResult.BONUS -> Triple(
            GameColors.StarYellow,
            Color(0xFF1E1E1E),
            null,
        )
        com.wheelword.game.AttemptResult.DUPLICATE -> Triple(
            GameColors.BadgeBlue,
            Color.White,
            null,
        )
        com.wheelword.game.AttemptResult.INVALID -> Triple(
            Color(0x55000000),
            Color(0xC0FFFFFF),
            androidx.compose.ui.text.style.TextDecoration.LineThrough,
        )
        com.wheelword.game.AttemptResult.TOO_SHORT -> Triple(
            Color(0x33000000),
            Color(0x99FFFFFF),
            null,
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = attempt.word,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = decoration,
        )
    }
}

/**
 * Floating SPIN button. 64dp gold gradient circle anchored bottom-right
 * of the play area — Material's FAB conventional spot. The little gift
 * emoji + "SPIN" caption underneath keeps the affordance obvious.
 *
 * Only displayed on days when the daily spin is still available; the
 * caller controls visibility.
 */
@Composable
private fun FloatingSpinButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = androidx.compose.foundation.shape.CircleShape)
                .size(58.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(GameColors.GemGreen, Color(0xFF1F8030)),
                    ),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🎁",
                fontSize = 30.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "SPIN",
            color = Color.White,
            fontSize = 11.sp,
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

@Composable
private fun FloatingHelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Bottom-left anchored. Smaller than the FAB-style SPIN button on
    // the right because help is reference info, not a primary action.
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x99000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
