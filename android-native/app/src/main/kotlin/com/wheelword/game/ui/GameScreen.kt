package com.wheelword.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
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
    // Trimmed across the board: real-device feedback was that the
    // grid + wheel were losing usable area to outer / between-element
    // padding. Floors stay where they are (8 / 4 on small phones)
    // but the regular sizes are tighter than the previous defaults.
    return Spec(
        outerH = if (short < 360.dp) 4.dp else 8.dp,
        outerV = if (compact) 6.dp else 10.dp,
        gapAfterTopBar = if (compact) 4.dp else 8.dp,
        gapAfterGrid = if (compact) 2.dp else 4.dp,
        gapAfterWord = if (compact) 0.dp else 2.dp,
        gapBeforeButtons = if (compact) 2.dp else 4.dp,
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
    var coinInfoOpen by remember { mutableStateOf(false) }
    // Help dialog: open by default for first-time players (storage.seenHelp
    // == false). Once the player dismisses, we flip the flag persistently
    // so it doesn't pop again on subsequent launches.
    var helpOpen by remember { mutableStateOf(storage?.seenHelp == false) }
    // App opens on the home screen; tap "LEVEL X" to enter the puzzle.
    var atHome by rememberSaveable { mutableStateOf(true) }
    // Intro flow (design v2): branded splash → loading bar over the
    // level art → home. Saveable so a config change doesn't replay it.
    var intro by rememberSaveable { mutableStateOf("splash") }

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

    // Branded intro: splash (wheel mark + wordmark) then the loading
    // screen over the upcoming level's background.
    when (intro) {
        "splash" -> {
            SplashIntroScreen(onDone = { intro = "loading" })
            return
        }
        "loading" -> {
            LoadingScreen(level = game.levelNum, onDone = { intro = "done" })
            return
        }
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
                alreadySpun = !game.canSpinToday(today),
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
        // Legibility gradient from the design: darker at the very top
        // (HUD) and bottom (wheel/buttons), clear through the middle.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x8C050C1E),
                        0.3f to Color(0x26050C1E),
                        0.55f to Color(0x1A050C1E),
                        1f to Color(0x80050C1E),
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
                    onSettingsClick = { settingsOpen = true },
                    onCoinClick = { coinInfoOpen = true },
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
                    onSettingsClick = { settingsOpen = true },
                    onCoinClick = { coinInfoOpen = true },
                )
            }

            // Bottom row of floating buttons: help (left), SPIN (centre,
            // when available), hint (right). Hint and SPIN swapped from
            // their previous positions — hint is the primary in-game
            // action, so it gets the thumb-friendly bottom-right; SPIN
            // is occasional / daily, so it goes centre.
            FloatingHelpButton(
                onClick = { helpOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 24.dp),
            )

            // SPIN stays visible every day (the dialog says "come back
            // tomorrow" once claimed); the red dot marks availability.
            FloatingSpinButton(
                available = game.canSpinToday(today),
                onClick = { spinDialogOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )

            FloatingHintButton(
                hintsLeft = game.hintsLeft,
                wordsTowardHint = game.wordsTowardHint,
                coins = game.coins,
                onHint = {
                    status = game.hintRevealRandomLetter()
                    playForStatus(status)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 12.dp),
            )
        }

        if (spinDialogOpen) {
            SpinWheelDialog(
                alreadySpun = !game.canSpinToday(today),
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
                levelNum = game.levelNum,
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

        if (coinInfoOpen) {
            CoinInfoDialog(
                coins = game.coins,
                onDismiss = { coinInfoOpen = false },
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
    onSettingsClick: () -> Unit,
    onCoinClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
    ) {
        // Wheel size is locked to a screen-width fraction — *not* derived
        // from "leftover" Column space — so it never shrinks when the
        // grid grows for harder levels. 0.88 of width (down from 0.96):
        // user feedback was that a wider wheel was crowding the
        // WordPreview pill above and the chip strip below; reducing
        // diameter while pushing tiles closer to the visible disc edge
        // (see LetterWheel — tileOrbit 0.62 → 0.72) keeps the *visible*
        // wheel area roughly the same but recovers vertical space.
        val wheelSize = minOf(maxWidth * 0.88f, maxHeight * 0.40f)
            .coerceIn(260.dp, 460.dp)

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
                        onCoinClick = onCoinClick,
                    )
                }
                Spacer(Modifier.width(8.dp))
                SettingsIconButton(onClick = onSettingsClick)
            }
            Spacer(Modifier.height(spec.gapAfterTopBar))
            LocationChip(level = level)
            Spacer(Modifier.height(spec.gapAfterTopBar))

            // Grid takes the leftover vertical space ABOVE the wheel.
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            WordPreview(text = game.currentWord())

            LetterWheel(
                tiles = game.tiles,
                selection = game.selection,
                onSubmit = onSubmitWheel,
                onShuffle = onShuffle,
                modifier = Modifier.size(wheelSize),
            )

            // Word history sits BELOW the wheel as a compact strip.
            RecentAttemptsRow(attempts = game.recentAttempts)

            // Reserve room at the bottom of the column for the row of
            // floating buttons (help / SPIN / hint) so they don't overlap
            // the chip strip above. 80 dp ≈ floating disc 64 + caption 4
            // + bottom padding 24 - some overlap, balanced against
            // not eating wheel real estate.
            Spacer(Modifier.height(FloatingButtonReserveHeight))
        }

        // Status notification — pure overlay, NEVER part of the column
        // flow. Floats just above the wheel's top edge at a fixed offset
        // from the bottom, so the grid stays rock-steady regardless of
        // whether status is showing.
        StatusOverlay(
            status = status,
            fontSp = spec.statusFontSp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Stack from the bottom: floating-buttons reserve + recent
                // strip + wheel + 8 buffer. Keeps status visually just
                // above the wheel's top edge.
                .padding(bottom = wheelSize + RecentAttemptsSlotHeight + FloatingButtonReserveHeight + 8.dp)
                .zIndex(2f),
        )
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
    onSettingsClick: () -> Unit,
    onCoinClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spec.outerH, vertical = spec.outerV),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    onCoinClick = onCoinClick,
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
                // Status now renders as overlay at the LandscapeContent
                // outer Box level — see end of function.
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
                // Reserve space for the floating button row at the bottom
                // (matches PortraitContent layout).
                Spacer(Modifier.height(FloatingButtonReserveHeight))
            }
        }
        }  // end Column

        // Status overlay — landscape positions it center-screen between
        // the grid (left) and wheel (right) columns. Same principle as
        // portrait: never affects layout flow.
        StatusOverlay(
            status = status,
            fontSp = spec.statusFontSp,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(2f),
        )
    }
}

/* ---------- Small reusable bits ---------- */

// Fixed-height wrappers — the wheel size below is computed from the
// remaining vertical space, so any slot whose height varies frame-to-frame
// would resize the wheel as the player types or as a status message
// appears. Reserving a constant height per slot keeps the wheel rock-solid.
// 30 dp fits the 18 sp Baloo pill (text natural ~25 dp + 2 dp pad)
// with breathing room.
private val WordPreviewSlotHeight = 30.dp
// StatusSlotHeight removed — status now renders as an absolute overlay.
// 24 dp fits a 12 sp chip with 3 dp vertical padding cleanly.
private val RecentAttemptsSlotHeight = 24.dp
private const val RECENT_ATTEMPTS_SHOWN = 3
// Floating button row at the bottom — help (left), SPIN (centre, when
// available), hint (right) — sits OVER this much space. The column
// reserves a Spacer of this height so chips above don't slide under
// the buttons. 80 dp = 64 dp disc + ~16 dp visual margin.
private val FloatingButtonReserveHeight = 80.dp

@Composable
private fun WordPreview(text: String) {
    // Live-trace pill in the design's gold accent with chunky white
    // letters. Sits inside the fixed WordPreviewSlotHeight so the wheel
    // below never moves as the player drags.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WordPreviewSlotHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(GameColors.Gold)
                    .padding(horizontal = 16.dp, vertical = 1.dp),
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontFamily = com.wheelword.game.theme.BalooFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                )
            }
        }
    }
}

/**
 * Centered location chip under the HUD ("VIETNAM", "BALI", …) —
 * the design's uppercase, letter-spaced country label.
 */
@Composable
private fun LocationChip(level: Int) {
    val name = countryNameFor(level) ?: return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x73050C1E))
            .border(width = 1.dp, color = GameColors.StrokeSoft, shape = RoundedCornerShape(50)),
    ) {
        Text(
            text = name.uppercase(),
            color = GameColors.InkSoft,
            fontFamily = com.wheelword.game.theme.NunitoFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        )
    }
}

/** Maps a GameState status message to its toast accent colour —
 *  green for grid words, gold for bonuses, blue for dupes/info, red
 *  for misses — mirroring the design's toast types. */
private fun toastColorFor(status: String): Color = when {
    status.startsWith("Found:") -> GameColors.Green
    status.startsWith("Bonus:") -> GameColors.Gold
    status.startsWith("No match") -> GameColors.Red
    status.startsWith("Already") || status.startsWith("Bonus already") -> GameColors.Blue
    else -> GameColors.Blue
}

@Composable
private fun StatusOverlay(
    status: String,
    fontSp: Int,
    modifier: Modifier = Modifier,
) {
    // Pure overlay: returns nothing when status is empty, and the caller
    // is responsible for absolute positioning via [modifier]. This pill
    // never participates in any column layout — that's the whole point;
    // grids and wheels nearby must not move when it appears or disappears.
    if (status.isEmpty()) return
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(toastColorFor(status))
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Text(
            text = status,
            color = Color.White,
            fontFamily = com.wheelword.game.theme.BalooFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSp.sp,
            maxLines = 1,
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
            GameColors.Green,
            Color.White,
            null,
        )
        com.wheelword.game.AttemptResult.BONUS -> Triple(
            GameColors.Gold,
            Color.White,
            null,
        )
        com.wheelword.game.AttemptResult.DUPLICATE -> Triple(
            GameColors.Blue,
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
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = attempt.word,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = decoration,
        )
    }
}

/**
 * Floating SPIN button: green candy disc with the gift emoji, a
 * "SPIN" caption, and a red availability dot when today's spin is
 * unclaimed — straight from the design's bottom action row.
 */
@Composable
private fun FloatingSpinButton(
    available: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .shadow(elevation = 10.dp, shape = CircleShape)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GameColors.Green, GameColors.GreenDeep),
                        ),
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🎁",
                    fontSize = 26.sp,
                )
            }
            if (available) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(GameColors.Red)
                        .border(width = 2.dp, color = Color.White, shape = CircleShape),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "SPIN",
            color = Color.White,
            fontFamily = com.wheelword.game.theme.BalooFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0xB3000000),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 6f,
                ),
            ),
        )
    }
}

@Composable
private fun SettingsIconButton(onClick: () -> Unit) {
    RoundGlassButton(onClick = onClick, size = 38.dp) {
        Text(text = "⚙", color = Color.White, fontSize = 19.sp)
    }
}

@Composable
private fun FloatingHelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Bottom-left anchored glass disc, matching the design's "?" button.
    RoundGlassButton(onClick = onClick, size = 42.dp, modifier = modifier) {
        Text(
            text = "?",
            color = Color.White,
            fontFamily = com.wheelword.game.theme.BalooFamily,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/**
 * Bottom-right anchored hint button. Mirrors the layout of the in-flow
 * BottomButtons.HintButton (gold disc + count badge + progress bar) but
 * lives as a floating overlay so it doesn't reserve column space —
 * which means the wheel and chip strip get to claim everything above
 * the floating-button row.
 */
@Composable
private fun FloatingHintButton(
    hintsLeft: Int,
    wordsTowardHint: Int,
    coins: Int,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canBuyWithCoins = hintsLeft == 0 && coins >= com.wheelword.game.HINT_COIN_COST
    val available = hintsLeft > 0 || canBuyWithCoins

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(78.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomStart)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (available) Brush.verticalGradient(
                            colors = listOf(GameColors.GoldLight, GameColors.Gold),
                        )
                        else Brush.verticalGradient(
                            colors = listOf(Color(0xFF555F75), Color(0xFF424B5E)),
                        )
                    )
                    .clickable(onClick = onHint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "💡",
                    fontSize = 28.sp,
                )
            }
            // Count badge: green digit when free hints remain; gold
            // "50" when the player can buy with coins; muted "50" when
            // they can't (still tappable so they get the "Need X coins"
            // toast).
            val badgeBg = when {
                hintsLeft > 0 -> GameColors.Green
                canBuyWithCoins -> GameColors.GoldDeep
                else -> Color(0xFF787878)                     // gray (can't afford)
            }
            val badgeText: String = if (hintsLeft > 0) hintsLeft.toString()
                                    else com.wheelword.game.HINT_COIN_COST.toString()
            // Slightly smaller font when showing the price so two
            // digits fit comfortably inside the 28 dp ring.
            val badgeFont = if (hintsLeft > 0) 14.sp else 12.sp
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(width = 2.5.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontFamily = com.wheelword.game.theme.BalooFamily,
                    fontSize = badgeFont,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        // Progress toward the next free hint (every 10 words found).
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .width(72.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x66091226)),
        ) {
            val progress = (wordsTowardHint.coerceAtMost(10)) / 10f
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(GameColors.Green),
                )
            }
        }
    }
}
