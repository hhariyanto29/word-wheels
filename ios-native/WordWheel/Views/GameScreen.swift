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
                    onSpinResult: { sec in
                        game.applySpinReward(
                            today: todayEpochDay,
                            coinsAdded: sec.coins,
                            hintsAdded: sec.hints,
                        )
                        if sec.coins > 0 { sound.play(.wordFound) }
                        else if sec.hints > 0 { sound.play(.hint) }
                    },
                    onDismiss: { spinDialogOpen = false },
                )
            }

            if settingsOpen {
                SettingsDialog(sound: sound, onDismiss: { settingsOpen = false })
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
                if game.canSpinToday(today: todayEpochDay) {
                    Button(action: { spinDialogOpen = true }) {
                        Text("🎁 SPIN")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12).padding(.vertical, 8)
                            .background(RoundedRectangle(cornerRadius: 22)
                                .fill(GameColors.gemGreen))
                    }
                    .buttonStyle(.plain)
                }
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
            Spacer().frame(height: spec.gapAfterGrid)

            wordPreview
            Spacer().frame(height: spec.gapAfterWord)
            statusBubble(spec: spec)

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

            if !game.bonusFound.isEmpty {
                Text("Bonus: \(game.bonusFound.joined(separator: ", "))")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.627))
                    .padding(.top, 4)
            }
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
                if game.canSpinToday(today: todayEpochDay) {
                    Button(action: { spinDialogOpen = true }) {
                        Text("🎁 SPIN")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12).padding(.vertical, 8)
                            .background(RoundedRectangle(cornerRadius: 22)
                                .fill(GameColors.gemGreen))
                    }
                    .buttonStyle(.plain)
                }
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
                    if !game.bonusFound.isEmpty {
                        Text("Bonus: \(game.bonusFound.joined(separator: ", "))")
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.627))
                            .padding(.top, 4)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, spec.outerH)
        .padding(.vertical, spec.outerV)
    }

    // MARK: - Reusable bits

    @ViewBuilder private var wordPreview: some View {
        if !game.currentWord().isEmpty {
            Text(game.currentWord())
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(GameColors.letterColor)
                .padding(.horizontal, 20).padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 18).fill(GameColors.wheelBg))
        } else {
            Spacer().frame(height: 36)
        }
    }

    @ViewBuilder private func statusBubble(spec: Spec) -> some View {
        if !status.isEmpty {
            Text(status)
                .font(.system(size: CGFloat(spec.statusFontSp)))
                .foregroundColor(.white)
                .padding(.horizontal, 14).padding(.vertical, 6)
                .background(RoundedRectangle(cornerRadius: 14).fill(Color.black.opacity(0.549)))
        } else {
            Spacer().frame(height: 30)
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
}

private func spec(for size: CGSize, landscape: Bool) -> Spec {
    let short = min(size.width, size.height)
    let compact = size.height < 680 || (landscape && size.height < 420)
    return Spec(
        outerH: short < 360 ? 10 : 16,
        outerV: compact ? 12 : 24,
        gapAfterTopBar: compact ? 8 : 16,
        gapAfterGrid: compact ? 8 : 14,
        gapAfterWord: compact ? 4 : 6,
        gapBeforeButtons: compact ? 4 : 8,
        statusFontSp: short < 360 ? 13 : 15,
    )
}
