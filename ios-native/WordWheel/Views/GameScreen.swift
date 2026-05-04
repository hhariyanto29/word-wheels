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
                // HOPEWELL WHELD WORD background — scaled to fill
                Image("GameBackground")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
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
                // SPIN moved to a floating button so it never squishes
                // the coin / word / level labels inside the TopBar row.
                Button(action: { helpOpen = true }) {
                    Text("?")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .background(Circle().fill(Color.white.opacity(0.25)))
                }
                .buttonStyle(.plain)
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

            CrosswordGrid(level: game.level, visible: game.visibleLetters(),
                          usedCells: game.usedCells)
                .frame(maxWidth: .infinity)
                .aspectRatio(CGFloat(game.level.cols) / CGFloat(game.level.rows), contentMode: .fit)
                .frame(maxHeight: spec.gridMaxHeight)
            Spacer().frame(height: spec.gapAfterGrid)

            wordPreview
            Spacer().frame(height: spec.gapAfterWord)
            statusBubble(spec: spec)
            recentAttemptsRow

            Spacer(minLength: 0)
            GeometryReader { box in
                let side = min(box.size.width, box.size.height)
                LetterWheel(tiles: game.tiles, selection: $game.selection,
                            onSubmit: handleWheelSubmit, onShuffle: { game.shuffleTiles() })
                    .frame(width: side, height: side)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            Spacer(minLength: 0)

            Spacer().frame(height: spec.gapBeforeButtons)
            BottomButtons(hintsLeft: game.hintsLeft,
                          wordsTowardHint: game.wordsTowardHint,
                          onHint: handleHint)

            // Always reserve the bonus row's height so finding a bonus
            // word doesn't reflow the wheel column.
            Spacer().frame(height: 4)
            bonusRow
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
                // SPIN moved to a floating button so it never squishes
                // the coin / word / level labels inside the TopBar row.
                Button(action: { helpOpen = true }) {
                    Text("?")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 38, height: 38)
                        .background(Circle().fill(Color.white.opacity(0.25)))
                }
                .buttonStyle(.plain)
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
                    Spacer().frame(height: spec.gapAfterWord)
                    statusBubble(spec: spec)
                    recentAttemptsRow
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)

                // Right column: wheel + buttons
                VStack(spacing: 0) {
                    GeometryReader { box in
                        let side = min(box.size.width, box.size.height)
                        LetterWheel(tiles: game.tiles, selection: $game.selection,
                                    onSubmit: handleWheelSubmit, onShuffle: { game.shuffleTiles() })
                            .frame(width: side, height: side)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                    Spacer().frame(height: spec.gapBeforeButtons)
                    BottomButtons(hintsLeft: game.hintsLeft,
                                  wordsTowardHint: game.wordsTowardHint,
                                  onHint: handleHint)
                    // Always reserve the bonus row's height so finding
                    // a bonus word doesn't reflow the wheel column.
                    Spacer().frame(height: 4)
                    bonusRow
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
    private static let wordPreviewSlotHeight: CGFloat = 40
    private static let statusSlotHeight: CGFloat = 32
    private static let bonusRowSlotHeight: CGFloat = 22
    private static let recentAttemptsSlotHeight: CGFloat = 32
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

    @ViewBuilder private func statusBubble(spec: Spec) -> some View {
        ZStack {
            if !status.isEmpty {
                Text(status)
                    .font(.system(size: CGFloat(spec.statusFontSp)))
                    .foregroundColor(.white)
                    .padding(.horizontal, 14).padding(.vertical, 6)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Color.black.opacity(0.549)))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.statusSlotHeight)
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
                HStack(spacing: 6) {
                    ForEach(game.recentAttempts.prefix(Self.recentAttemptsShown)) { a in
                        attemptChip(a)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.recentAttemptsSlotHeight)
    }

    @ViewBuilder private func attemptChip(_ a: Attempt) -> some View {
        let bg: Color
        let fg: Color
        let strike: Bool
        switch a.result {
        case .grid:      bg = GameColors.gemGreen;        fg = .white;                    strike = false
        case .bonus:     bg = GameColors.starYellow;      fg = GameColors.letterColor;    strike = false
        case .duplicate: bg = GameColors.badgeBlue;       fg = .white;                    strike = false
        case .invalid:   bg = Color.black.opacity(0.33);  fg = Color.white.opacity(0.75); strike = true
        case .tooShort:  bg = Color.black.opacity(0.20);  fg = Color.white.opacity(0.60); strike = false
        }
        Text(a.word)
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(fg)
            .strikethrough(strike, color: fg)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(RoundedRectangle(cornerRadius: 10).fill(bg))
    }

    @ViewBuilder private var bonusRow: some View {
        ZStack {
            if !game.bonusFound.isEmpty {
                // .lineLimit(1) prevents the row from wrapping to a
                // second line and getting clipped by our fixed height.
                Text("Bonus: \(game.bonusFound.joined(separator: ", "))")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.627))
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .padding(.horizontal, 12)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.bonusRowSlotHeight)
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
    // Cap the grid at ~34% of column height in portrait so the wheel,
    // which uses leftover space, stays the dominant element. nil = no
    // cap (landscape uses a side-by-side layout instead).
    let gridMaxHeight: CGFloat?
}

private func spec(for size: CGSize, landscape: Bool) -> Spec {
    let short = min(size.width, size.height)
    let compact = size.height < 680 || (landscape && size.height < 420)
    return Spec(
        outerH: short < 360 ? 8 : 12,
        outerV: compact ? 12 : 24,
        gapAfterTopBar: compact ? 8 : 16,
        gapAfterGrid: compact ? 8 : 14,
        gapAfterWord: compact ? 4 : 6,
        gapBeforeButtons: compact ? 4 : 8,
        statusFontSp: short < 360 ? 13 : 15,
        gridMaxHeight: landscape ? nil : size.height * 0.34,
    )
}
