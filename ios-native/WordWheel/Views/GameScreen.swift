import SwiftUI

/// Top-level game screen. Handles level progression, coin/hint carry-over
/// between levels, sound routing for submit-based events, and switches to a
/// two-column layout in landscape orientation.
struct GameScreen: View {
    @EnvironmentObject private var sound: SoundManager

    /// Owned for the lifetime of the screen. Plain class, not observable —
    /// the only reader is `GameState.persist`.
    private let storage: GameStorage

    @StateObject private var game: GameState

    @State private var status: String = ""
    @State private var spinDialogOpen = false
    @State private var settingsOpen = false
    /// Help dialog state. Initialised from `storage.seenHelp` in `init`
    /// so first-time players see the modal automatically.
    @State private var helpOpen: Bool
    /// Spin reward is captured when the wheel stops, but not credited
    /// to the coin counter until the dialog dismisses — that way the
    /// TopBar's count-up + pulse animation plays visibly.
    @State private var pendingSpinReward: SpinSector? = nil
    @State private var pendingDifficultyTier: Difficulty? = nil
    @State private var pendingNextLevel: Int? = nil
    /// App opens on the home screen; tapping LEVEL X switches to the puzzle.
    @State private var atHome = true

    /// Local epoch day at the time the view was first built. Used as the
    /// "today" key for streak ticks and the daily-spin gate.
    private static func todayEpochDay() -> Int {
        let secs = Date().timeIntervalSince1970
        // Apply current timezone offset so the rollover happens at the
        // user's local midnight, not UTC midnight.
        let local = secs + Double(TimeZone.current.secondsFromGMT())
        return Int(local / 86400)
    }
    @State private var todayEpochDay: Int = GameScreen.todayEpochDay()

    init(storage: GameStorage = GameStorage()) {
        self.storage = storage
        _helpOpen = State(initialValue: !storage.seenHelp)

        let saved = storage.load()
        let initial = GameState(
            levelNum: saved?.levelNum ?? 1,
            initialCoins: saved?.coins ?? 200,
        )
        if let saved {
            initial.restore(saved)
        } else {
            initial.hintsLeft = 5
            initial.wordsTowardHint = 0
        }
        // Wire the persist callback — captures `storage` by value, and
        // the closure references `initial` weakly-ish via a trailing
        // closure that re-fetches the snapshot from the GameState itself.
        let capturedStorage = storage
        initial.persist = { [weak initial] in
            guard let s = initial else { return }
            capturedStorage.save(s.snapshot())
        }
        _game = StateObject(wrappedValue: initial)
    }

    var body: some View {
        ZStack {
            if atHome {
                HomeScreen(
                    levelNum: game.levelNum,
                    coins: game.coins,
                    streak: game.currentStreak,
                    spinAvailable: game.canSpinToday(today: todayEpochDay),
                    onResume: { atHome = false },
                    onSpinClick: { spinDialogOpen = true },
                    onSettingsClick: { settingsOpen = true },
                )
            } else {
                puzzleBody
            }

            if spinDialogOpen {
                SpinWheelDialog(
                    // Stash the result; we don't credit the player's
                    // account until the dialog dismisses. That way the
                    // TopBar's count-up + pulse animation actually
                    // plays visibly, because before dismiss the dialog
                    // covers the bar.
                    onSpinResult: { sec in pendingSpinReward = sec },
                    onDismiss: {
                        if let sec = pendingSpinReward {
                            game.applySpinReward(
                                today: todayEpochDay,
                                coinsAdded: sec.coins,
                                hintsAdded: sec.hints,
                            )
                            if sec.coins > 0 { sound.play(.wordFound) }
                            else if sec.hints > 0 { sound.play(.hint) }
                        }
                        pendingSpinReward = nil
                        spinDialogOpen = false
                    },
                )
            }

            if settingsOpen {
                SettingsDialog(sound: sound, onDismiss: { settingsOpen = false })
            }

            if helpOpen {
                HelpDialog(onDismiss: {
                    helpOpen = false
                    storage.seenHelp = true
                })
            }

            if let tier = pendingDifficultyTier {
                DifficultyDialog(tier: tier, onDismiss: {
                    let next = pendingNextLevel
                    pendingDifficultyTier = nil
                    pendingNextLevel = nil
                    if let n = next {
                        advanceTo(level: n)
                    }
                })
            }
        }
        .onAppear {
            game.tickDailyStreak(today: todayEpochDay)
        }
    }

    private var puzzleBody: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let landscape = w > h
            let spec = spec(for: geo.size, landscape: landscape)

            ZStack {
                // Per-level country background: Vietnam, Brunei,
                // Malaysia, Myanmar, Papua Nugini, Filipina, Singapore,
                // Thailand, Indonesia, Bali — one image per 10-level
                // range. Resolver lives in Backgrounds.swift.
                GameBackgroundImage(level: game.levelNum)
                    .frame(width: w, height: h)
                    .clipped()
                // Dark overlay for text legibility
                LinearGradient(
                    colors: [
                        Color(.sRGB, red: 0, green: 0, blue: 0.08, opacity: 0.4),
                        Color(.sRGB, red: 0, green: 0, blue: 0.16, opacity: 0.3),
                        Color(.sRGB, red: 0, green: 0, blue: 0.2,  opacity: 0.5),
                    ],
                    startPoint: .top, endPoint: .bottom,
                )
                .ignoresSafeArea()

                if landscape {
                    landscapeContent(spec: spec)
                } else {
                    portraitContent(spec: spec)
                }

                // Floating SPIN button — pinned bottom-right so it
                // never squishes the coin / word / level labels in
                // the TopBar row. Hidden on days when the daily spin
                // is no longer available.
                if game.canSpinToday(today: todayEpochDay) {
                    floatingSpinButton
                        .padding(.trailing, 16)
                        .padding(.bottom, 24)
                        .frame(maxWidth: .infinity, maxHeight: .infinity,
                               alignment: .bottomTrailing)
                }

                // Floating help — bottom-left, smaller than SPIN. Pulled
                // out of the TopBar so the level / words labels have
                // room to breathe (otherwise "Lv.2" was wrapping into a
                // vertical glyph stack on narrow widths).
                floatingHelpButton
                    .padding(.leading, 14)
                    .padding(.bottom, 26)
                    .frame(maxWidth: .infinity, maxHeight: .infinity,
                           alignment: .bottomLeading)

                // Status notification — pure overlay, NEVER part of any
                // VStack flow. Positioned just above the wheel's top
                // edge using a fixed offset from the bottom: wheel size
                // + recent-attempts slot + bottom buttons + a small
                // buffer. The grid no longer resizes when status pops.
                statusOverlay(spec: spec)
                    .padding(.bottom, spec.wheelSize
                             + Self.recentAttemptsSlotHeight
                             + 90      // BottomButtons (hint disc 78 + progress bar)
                             + 12)     // visual buffer
                    .frame(maxWidth: .infinity, maxHeight: .infinity,
                           alignment: .bottom)
                    .zIndex(2)

                if game.isComplete && pendingDifficultyTier == nil {
                    CompletionDialog(
                        isLastLevel: game.levelNum >= Level.totalLevels,
                        onNext: advanceLevel,
                    )
                }
            }
            .onChange(of: game.isComplete) { complete in
                if complete { sound.play(.complete) }
            }
            // Auto-clear status after a moment so the bubble doesn't
            // linger. With no reserved slot the column reflows around
            // its appearance/disappearance.
            .onChange(of: status) { newValue in
                guard !newValue.isEmpty else { return }
                let captured = newValue
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                    if status == captured { status = "" }
                }
            }
        }
    }

    // MARK: - Portrait

    @ViewBuilder
    private func portraitContent(spec: Spec) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                TopBar(coins: game.coins, found: game.found.count,
                       total: game.answers.count, level: game.levelNum,
                       streak: game.currentStreak)
                // Help + SPIN moved to floating buttons (see ZStack
                // overlay). Keeping the TopBar to {labels + settings}
                // only stops the labels from getting squeezed.
                Button(action: { settingsOpen = true }) {
                    Text("⚙")
                        .font(.system(size: 22))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .background(Circle().fill(Color.white.opacity(0.25)))
                }
                .buttonStyle(.plain)
            }
            Spacer().frame(height: spec.gapAfterTopBar)

            // Grid takes the leftover vertical space ABOVE the wheel.
            // No more height cap — the wheel is now a fixed absolute size,
            // so the grid no longer competes for it.
            CrosswordGrid(level: game.level, visible: game.visibleLetters(),
                          usedCells: game.usedCells)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .aspectRatio(CGFloat(game.level.cols) / CGFloat(game.level.rows), contentMode: .fit)
            Spacer().frame(height: spec.gapAfterGrid)

            wordPreview
            // Status now renders as a ZStack overlay at puzzleBody level
            // (see body) — never participates in this VStack flow, so
            // the grid never resizes when the bubble appears.

            // Wheel is a hard-locked square — same size regardless of
            // level / grid size / current word length.
            LetterWheel(tiles: game.tiles, selection: $game.selection,
                        onSubmit: handleWheelSubmit, onShuffle: { game.shuffleTiles() })
                .frame(width: spec.wheelSize, height: spec.wheelSize)

            // History strip directly under the wheel — out of the
            // wheel's way visually, easy to glance at after a submit.
            recentAttemptsRow

            BottomButtons(hintsLeft: game.hintsLeft,
                          wordsTowardHint: game.wordsTowardHint,
                          onHint: handleHint)
        }
        .padding(.horizontal, spec.outerH)
        .padding(.vertical, spec.outerV)
    }

    // MARK: - Landscape

    @ViewBuilder
    private func landscapeContent(spec: Spec) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                TopBar(coins: game.coins, found: game.found.count,
                       total: game.answers.count, level: game.levelNum,
                       streak: game.currentStreak)
                // Help + SPIN moved to floating buttons (see ZStack
                // overlay). Keeping the TopBar to {labels + settings}
                // only stops the labels from getting squeezed.
                Button(action: { settingsOpen = true }) {
                    Text("⚙")
                        .font(.system(size: 22))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .background(Circle().fill(Color.white.opacity(0.25)))
                }
                .buttonStyle(.plain)
            }
            Spacer().frame(height: spec.gapAfterTopBar)

            HStack(alignment: .center, spacing: 16) {
                // Left column: grid + status
                VStack(spacing: 0) {
                    CrosswordGrid(level: game.level, visible: game.visibleLetters(),
                                  usedCells: game.usedCells)
                        .frame(maxWidth: .infinity)
                        .aspectRatio(CGFloat(game.level.cols) / CGFloat(game.level.rows), contentMode: .fit)
                    Spacer().frame(height: spec.gapAfterGrid)
                    wordPreview
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)

                // Right column: wheel + history (under wheel) + buttons
                VStack(spacing: 0) {
                    LetterWheel(tiles: game.tiles, selection: $game.selection,
                                onSubmit: handleWheelSubmit, onShuffle: { game.shuffleTiles() })
                        .frame(width: spec.wheelSize, height: spec.wheelSize)
                    recentAttemptsRow
                    Spacer().frame(height: spec.gapBeforeButtons)
                    BottomButtons(hintsLeft: game.hintsLeft,
                                  wordsTowardHint: game.wordsTowardHint,
                                  onHint: handleHint)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, spec.outerH)
        .padding(.vertical, spec.outerV)
    }

    // MARK: - Reusable bits

    // Fixed-height wrappers — the wheel below uses a `GeometryReader`
    // that fills the remaining vertical space, so any slot whose height
    // varies frame-to-frame would resize the wheel as the player types
    // or as a status message appears. Reserving a constant height per
    // slot keeps the wheel rock-solid.
    private static let wordPreviewSlotHeight: CGFloat = 22
    // statusSlotHeight removed — status now renders as an absolute
    // ZStack overlay (see puzzleBody) so it never participates in any
    // VStack flow. Grids and wheels stay the same size whether status
    // is showing or not.
    private static let recentAttemptsSlotHeight: CGFloat = 18
    private static let recentAttemptsShown = 3

    @ViewBuilder private var wordPreview: some View {
        ZStack {
            if !game.currentWord().isEmpty {
                Text(game.currentWord())
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(GameColors.letterColor)
                    .padding(.horizontal, 20).padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 18).fill(GameColors.wheelBg))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.wordPreviewSlotHeight)
    }

    /// Pure overlay status pill. Caller positions it via the parent
    /// ZStack — this view never participates in any VStack flow, so
    /// the grid stays the same size whether status is showing or not.
    @ViewBuilder private func statusOverlay(spec: Spec) -> some View {
        if !status.isEmpty {
            Text(status)
                .font(.system(size: CGFloat(spec.statusFontSp)))
                .foregroundColor(.white)
                .padding(.horizontal, 14).padding(.vertical, 6)
                .background(RoundedRectangle(cornerRadius: 14).fill(Color.black.opacity(0.8)))
        }
    }

    /// Floating SPIN button. Gold-green gradient circle pinned to the
    /// bottom-right of the play area — Material's FAB convention.
    /// Visibility is gated by the caller (only on days the daily spin
    /// is still available).
    @ViewBuilder private var floatingSpinButton: some View {
        Button(action: { spinDialogOpen = true }) {
            VStack(spacing: 4) {
                ZStack {
                    Circle()
                        .fill(LinearGradient(
                            colors: [GameColors.gemGreen,
                                     Color(red: 0.12, green: 0.5, blue: 0.19)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing,
                        ))
                        .frame(width: 58, height: 58)
                        .shadow(color: .black.opacity(0.5), radius: 6, x: 0, y: 4)
                    Text("🎁")
                        .font(.system(size: 30))
                }
                Text("SPIN")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
            }
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder private var recentAttemptsRow: some View {
        ZStack {
            if !game.recentAttempts.isEmpty {
                HStack(spacing: 4) {
                    ForEach(game.recentAttempts.prefix(Self.recentAttemptsShown)) { a in
                        attemptChip(a)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.recentAttemptsSlotHeight)
    }

    /// Return type avoids `@ViewBuilder` because the switch statement
    /// produces a `()` value; ViewBuilder would try to interpret that as
    /// a view and fail with "type '()' cannot conform to 'View'". A plain
    /// function with an explicit `return` lets the let/switch flow
    /// naturally and only emits a single View at the end.
    private func attemptChip(_ a: Attempt) -> some View {
        let style = chipStyle(for: a.result)
        return Text(a.word)
            .font(.system(size: 11, weight: .semibold))
            .foregroundColor(style.fg)
            .strikethrough(style.strike, color: style.fg)
            .padding(.horizontal, 7).padding(.vertical, 2)
            .background(RoundedRectangle(cornerRadius: 8).fill(style.bg))
    }

    /// Compact help button — bottom-left anchored, smaller than the SPIN
    /// FAB on the right because help is reference info, not a primary
    /// action.
    @ViewBuilder private var floatingHelpButton: some View {
        Button(action: { helpOpen = true }) {
            Text("?")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 32, height: 32)
                .background(Circle().fill(Color.black.opacity(0.6)))
        }
        .buttonStyle(.plain)
    }

    private func chipStyle(for result: AttemptResult) -> (bg: Color, fg: Color, strike: Bool) {
        switch result {
        case .grid:      return (GameColors.gemGreen,        .white,                    false)
        case .bonus:     return (GameColors.starYellow,      GameColors.letterColor,    false)
        case .duplicate: return (GameColors.badgeBlue,       .white,                    false)
        case .invalid:   return (Color.black.opacity(0.33),  Color.white.opacity(0.75), true)
        case .tooShort:  return (Color.black.opacity(0.20),  Color.white.opacity(0.60), false)
        }
    }

    // MARK: - Event handlers

    private func handleWheelSubmit() {
        if game.currentWord().count >= 2 {
            status = game.trySubmit()
            playForStatus(status)
        } else {
            game.clearSelection()
        }
    }

    private func handleHint() {
        status = game.hintRevealRandomLetter()
        playForStatus(status)
    }

    private func playForStatus(_ msg: String) {
        if msg.hasPrefix("Found:")        { sound.play(.wordFound) }
        else if msg.hasPrefix("Bonus:")   { sound.play(.bonus) }
        else if msg.hasPrefix("No match") { sound.play(.wrong) }
        else if msg.hasPrefix("Revealed") { sound.play(.hint) }
    }

    private func advanceLevel() {
        let completed = game.levelNum
        game.coins += 10
        let next = completed >= Level.totalLevels ? 1 : completed + 1

        // If this was a tier-boundary level, stage the difficulty banner
        // first; the actual level transition runs after the banner is
        // dismissed (handled in the body's DifficultyDialog onDismiss).
        if let tier = Difficulty.milestone(for: completed) {
            pendingNextLevel = next
            pendingDifficultyTier = tier
        } else {
            advanceTo(level: next)
        }
    }

    private func advanceTo(level next: Int) {
        game.resetTo(
            level: next,
            carriedCoins: game.coins,
            carriedHints: game.hintsLeft,
            carriedWordsTowardHint: game.wordsTowardHint,
        )
        status = ""
    }
}

// MARK: - Layout spec

private struct Spec {
    let outerH: CGFloat
    let outerV: CGFloat
    let gapAfterTopBar: CGFloat
    let gapAfterGrid: CGFloat
    let gapAfterWord: CGFloat
    let gapBeforeButtons: CGFloat
    let statusFontSp: Int
    // Wheel side length is locked to a fraction of screen size so it
    // never shrinks when the grid grows for harder levels. Computed
    // once per layout pass from the screen size.
    let wheelSize: CGFloat
}

private func spec(for size: CGSize, landscape: Bool) -> Spec {
    let short = min(size.width, size.height)
    let compact = size.height < 680 || (landscape && size.height < 420)
    // Wheel scale bumped 0.92 -> 0.96 to honour the user's "wheel must
    // stay big" mandate after real-device testing.
    let wheelSide = min(size.width * 0.96, size.height * 0.44)
    return Spec(
        // Padding / gap trimmed across the board. Real-device screenshots
        // showed roughly ~80 pt of vertical space being eaten by margins
        // alone; cutting them in half gives the grid the breathing room
        // it needed without shrinking the wheel.
        outerH: short < 360 ? 4 : 8,
        outerV: compact ? 6 : 10,
        gapAfterTopBar: compact ? 4 : 8,
        gapAfterGrid: compact ? 2 : 4,
        gapAfterWord: compact ? 0 : 2,
        gapBeforeButtons: compact ? 2 : 4,
        statusFontSp: short < 360 ? 13 : 15,
        wheelSize: max(280, min(wheelSide, 480)),
    )
}
