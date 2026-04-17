import SwiftUI

/// Top-level game screen. Handles level progression, coin/hint carry-over
/// between levels, sound routing for submit-based events, and switches to a
/// two-column layout in landscape orientation.
struct GameScreen: View {
    @EnvironmentObject private var sound: SoundManager

    @State private var currentLevel: Int = 1
    @State private var carriedCoins: Int = 200
    @State private var carriedHints: Int = 5
    @State private var carriedWordsTowardHint: Int = 0

    @StateObject private var game: GameState = GameState(levelNum: 1, initialCoins: 200)

    @State private var status: String = ""

    var body: some View {
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

                if game.isComplete {
                    CompletionDialog(
                        isLastLevel: currentLevel >= Level.totalLevels,
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
            TopBar(coins: game.coins, found: game.found.count,
                   total: game.answers.count, level: currentLevel)
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
            BottomButtons(hintsLeft: game.hintsLeft, wordsTowardHint: game.wordsTowardHint,
                          onHint: handleHint, onSubmit: handleSubmit, onBackspace: handleBackspace)

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
            TopBar(coins: game.coins, found: game.found.count,
                   total: game.answers.count, level: currentLevel)
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
                    BottomButtons(hintsLeft: game.hintsLeft, wordsTowardHint: game.wordsTowardHint,
                                  onHint: handleHint, onSubmit: handleSubmit, onBackspace: handleBackspace)
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

    private func handleSubmit() {
        status = game.trySubmit()
        playForStatus(status)
    }

    private func handleHint() {
        status = game.hintRevealRandomLetter()
        playForStatus(status)
    }

    private func handleBackspace() {
        if !game.selection.isEmpty {
            sound.play(.backspace)
            game.backspace()
        }
    }

    private func playForStatus(_ msg: String) {
        if msg.hasPrefix("Found:")        { sound.play(.wordFound) }
        else if msg.hasPrefix("Bonus:")   { sound.play(.bonus) }
        else if msg.hasPrefix("No match") { sound.play(.wrong) }
        else if msg.hasPrefix("Revealed") { sound.play(.hint) }
    }

    private func advanceLevel() {
        // Carry coins + hint progress into the next level (mirrors Android behaviour)
        game.coins += 10
        carriedCoins = game.coins
        carriedHints = game.hintsLeft
        carriedWordsTowardHint = game.wordsTowardHint
        let next = currentLevel >= Level.totalLevels ? 1 : currentLevel + 1
        currentLevel = next
        game.resetTo(
            level: next,
            carriedCoins: carriedCoins,
            carriedHints: carriedHints,
            carriedWordsTowardHint: carriedWordsTowardHint,
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
