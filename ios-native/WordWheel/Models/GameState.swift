import Foundation

enum AttemptResult { case grid, bonus, duplicate, invalid, tooShort }

struct Attempt: Identifiable {
    let id = UUID()
    let word: String
    let result: AttemptResult
}

private let recentAttemptsCap = 5

/// Cap on how many *dictionary-only* bonus words earn coins per level.
/// Curated `bonusWords` from level data are always rewarded; this only
/// limits the open-ended dictionary fallback to keep hint earn rate
/// tractable. 50 is a starting value — tune from playtesting.
private let dictBonusCoinCap = 50

/// Cost in coins to reveal one hint when the player's free-hint pool
/// is exhausted (`hintsLeft == 0`). Lets cash-rich players keep
/// advancing without waiting for the next free hint to accrue.
/// Surfaced as a top-level `let` (not `private`) so the UI can read
/// it directly.
let HINT_COIN_COST = 50

/// Single-level game state. An `ObservableObject` so SwiftUI views update
/// automatically when `@Published` properties change — analogous to Compose's
/// `mutableStateOf` on the Kotlin side.
///
/// Every state-mutating method calls `persist()` at the end so progress
/// survives an app kill/restart. The `persist` callback is supplied at
/// construction — typically a `GameStorage.save(snapshot())` closure
/// wired up by `GameScreen`.
final class GameState: ObservableObject {
    @Published private(set) var level: Level
    @Published private(set) var levelNum: Int

    // Derived-from-level structural data. Computed properties (rather than
    // cached lets) because `level` is mutable — `resetTo` swaps it during
    // level transitions and these need to reflect the new puzzle.
    var usedCells: Set<Cell> { level.usedCells }
    var answers: Set<String> { level.answersSet }
    var bonusWordsSet: Set<String> { Set(level.bonusWords) }

    // Progress
    @Published var found: [String] = []
    @Published var bonusFound: [String] = []
    @Published var revealed: [Cell: Character] = [:]
    /// Newest attempt first. Capped at `recentAttemptsCap`.
    @Published var recentAttempts: [Attempt] = []

    @Published private(set) var tiles: [Character]
    @Published var selection: [Int] = []

    @Published var coins: Int
    @Published var hintsLeft: Int = 5
    @Published var wordsTowardHint: Int = 0
    /// Counter of dictionary-only bonus words that earned coins this
    /// level. Reset on level transition; capped at `dictBonusCoinCap`.
    @Published private(set) var dictBonusEarned: Int = 0

    // ── Meta-progression (streak + daily spin) ──────────────────────
    @Published var lastPlayedEpochDay: Int = 0
    @Published var currentStreak: Int = 0
    @Published var lastSpinEpochDay: Int = 0

    /// Callback invoked after each mutating operation. Wired to a
    /// `GameStorage.save(_:)` closure by `GameScreen`.
    var persist: () -> Void = {}

    init(levelNum: Int, initialCoins: Int) {
        let lvl = Level.get(levelNum)
        self.level = lvl
        self.levelNum = levelNum
        self.tiles = lvl.letters
        self.coins = initialCoins
    }

    func currentWord() -> String {
        String(selection.compactMap { idx in
            tiles.indices.contains(idx) ? tiles[idx] : nil
        })
    }

    func clearSelection() { selection.removeAll() }

    func backspace() {
        if !selection.isEmpty { selection.removeLast() }
    }

    func shuffleTiles() {
        clearSelection()
        tiles.shuffle()
        persist()
    }

    var isComplete: Bool { found.count == answers.count }

    /// Merges revealed hints with letters from words the player has already
    /// found. Used by `CrosswordGrid` when rendering.
    func visibleLetters() -> [Cell: Character] {
        var map = revealed
        for pw in level.words where found.contains(pw.word) {
            for (i, ch) in pw.word.enumerated() {
                let cell: Cell
                switch pw.dir {
                case .across: cell = Cell(row: pw.row, col: pw.col + i)
                case .down:   cell = Cell(row: pw.row + i, col: pw.col)
                }
                map[cell] = ch
            }
        }
        return map
    }

    /// Returns a status message for the UI after a submit attempt.
    func trySubmit() -> String {
        let guess = currentWord()
        if guess.count < 2 {
            recordAttempt(word: guess, result: .tooShort)
            return "Make a longer word."
        }

        if answers.contains(guess) {
            if !found.contains(guess) {
                found.append(guess)
                coins += 2
                let hintMsg = awardProgress()
                clearSelection()
                recordAttempt(word: guess, result: .grid)
                persist()
                return "Found: \(guess) (+2 pts)\(hintMsg)"
            } else {
                clearSelection()
                recordAttempt(word: guess, result: .duplicate)
                return "Already found."
            }
        }

        if bonusWordsSet.contains(guess) {
            if !bonusFound.contains(guess) {
                bonusFound.append(guess)
                coins += 2
                let hintMsg = awardProgress()
                clearSelection()
                recordAttempt(word: guess, result: .bonus)
                persist()
                return "Bonus: \(guess) (+2 pts)\(hintMsg)"
            } else {
                clearSelection()
                recordAttempt(word: guess, result: .duplicate)
                return "Bonus already counted."
            }
        }

        // Dictionary fallback (Option C hybrid). The wheel selection
        // mechanism guarantees the word is spellable from the wheel,
        // so we only need a dictionary membership check. First N earn
        // coins/hint progress; the rest are recognised but gated to
        // preserve hint pacing.
        if WordDictionary.contains(guess) {
            if !bonusFound.contains(guess) {
                bonusFound.append(guess)
                if dictBonusEarned < dictBonusCoinCap {
                    coins += 2
                    dictBonusEarned += 1
                    let hintMsg = awardProgress()
                    clearSelection()
                    recordAttempt(word: guess, result: .bonus)
                    persist()
                    return "Bonus: \(guess) (+2 pts)\(hintMsg)"
                } else {
                    clearSelection()
                    recordAttempt(word: guess, result: .bonus)
                    persist()
                    return "Bonus: \(guess) (cap reached)"
                }
            } else {
                clearSelection()
                recordAttempt(word: guess, result: .duplicate)
                return "Bonus already counted."
            }
        }

        clearSelection()
        recordAttempt(word: guess, result: .invalid)
        return "No match."
    }

    private func recordAttempt(word: String, result: AttemptResult) {
        // Skip empty/sub-2-letter prefixes — they're noise the player
        // didn't intend to "try".
        guard word.count >= 2 else { return }
        recentAttempts.insert(Attempt(word: word, result: result), at: 0)
        if recentAttempts.count > recentAttemptsCap {
            recentAttempts.removeLast(recentAttempts.count - recentAttemptsCap)
        }
    }

    private func awardProgress() -> String {
        wordsTowardHint += 1
        if wordsTowardHint >= 10 {
            wordsTowardHint = 0
            hintsLeft += 1
            return " +1 hint!"
        }
        return ""
    }

    /// In-place replacement of per-level state. Used by `GameScreen` when the
    /// player advances, because SwiftUI `@StateObject` only constructs its
    /// value once — we can't reassign `game`, so we mutate its fields.
    func resetTo(level newLevel: Int, carriedCoins: Int, carriedHints: Int, carriedWordsTowardHint: Int) {
        let lvl = Level.get(newLevel)
        self.level = lvl
        self.levelNum = newLevel
        self.tiles = lvl.letters
        self.selection = []
        self.found = []
        self.bonusFound = []
        self.revealed = [:]
        self.recentAttempts = []
        self.dictBonusEarned = 0
        self.coins = carriedCoins
        self.hintsLeft = carriedHints
        self.wordsTowardHint = carriedWordsTowardHint
        persist()
    }

    func hintRevealRandomLetter() -> String {
        // Three states:
        //   1. hintsLeft > 0           → use one free hint
        //   2. hintsLeft == 0,
        //      coins  >= HINT_COIN_COST → buy a hint with coins
        //   3. hintsLeft == 0,
        //      coins  <  HINT_COIN_COST → not enough; bail with msg
        let canUseFree = hintsLeft > 0
        let canBuyWithCoins = !canUseFree && coins >= HINT_COIN_COST
        if !canUseFree && !canBuyWithCoins {
            return "Need \(HINT_COIN_COST) coins to buy a hint."
        }
        let visible = visibleLetters()
        let candidates = usedCells.filter { visible[$0] == nil }
        guard let pick = candidates.randomElement(),
              let ch = level.solutionLetter(row: pick.row, col: pick.col)
        else { return candidates.isEmpty ? "No letters left to reveal." : "Hint failed." }
        revealed[pick] = ch
        if canUseFree {
            hintsLeft -= 1
        } else {
            coins -= HINT_COIN_COST
        }
        persist()
        return canUseFree ? "Revealed a letter!"
                          : "Bought a hint (-\(HINT_COIN_COST))"
    }

    // MARK: - Snapshot / restore

    /// Snapshot of everything that should survive an app restart.
    func snapshot() -> GameSnapshot {
        let reveal: [GameSnapshot.RevealedCell] = revealed.map { (cell, ch) in
            GameSnapshot.RevealedCell(row: cell.row, col: cell.col, ch: String(ch))
        }
        return GameSnapshot(
            levelNum: levelNum,
            coins: coins,
            hintsLeft: hintsLeft,
            wordsTowardHint: wordsTowardHint,
            tiles: String(tiles),
            found: found,
            bonusFound: bonusFound,
            revealed: reveal,
            lastPlayedEpochDay: lastPlayedEpochDay,
            currentStreak: currentStreak,
            lastSpinEpochDay: lastSpinEpochDay,
        )
    }

    // MARK: - Streak

    /// Call once on app open. Bumps the streak if exactly one day has
    /// passed since the last play, resets to 1 on bigger gaps, and is
    /// a no-op if the player already opened the app today.
    func tickDailyStreak(today: Int) {
        if lastPlayedEpochDay == today { return }
        if lastPlayedEpochDay == 0 {
            currentStreak = 1
        } else if today - lastPlayedEpochDay == 1 {
            currentStreak += 1
        } else {
            currentStreak = 1  // missed a day — reset
        }
        lastPlayedEpochDay = today
        persist()
    }

    // MARK: - Daily spin

    func canSpinToday(today: Int) -> Bool { lastSpinEpochDay != today }

    func applySpinReward(today: Int, coinsAdded: Int = 0, hintsAdded: Int = 0) {
        coins += coinsAdded
        hintsLeft += hintsAdded
        lastSpinEpochDay = today
        persist()
    }

    /// Rehydrate from a previously-saved snapshot. Defensive — if the saved
    /// tile multiset doesn't match the current level's letters (e.g. level
    /// data was edited in an update), fall back to the level's original
    /// order. Likewise, only restore `found` / `bonusFound` words that are
    /// still valid for this level.
    func restore(_ s: GameSnapshot) {
        let newLevel = max(1, min(s.levelNum, Level.totalLevels))
        let lvl = Level.get(newLevel)
        self.level = lvl
        self.levelNum = newLevel

        let savedTiles = Array(s.tiles)
        self.tiles = savedTiles.sorted() == lvl.letters.sorted()
            ? savedTiles
            : lvl.letters

        let answersSet = lvl.answersSet
        self.found = s.found.filter { answersSet.contains($0) }

        let bonusSet = Set(lvl.bonusWords)
        self.bonusFound = s.bonusFound.filter { bonusSet.contains($0) }

        let used = lvl.usedCells
        var rev: [Cell: Character] = [:]
        for r in s.revealed {
            let cell = Cell(row: r.row, col: r.col)
            if used.contains(cell), let c = r.ch.first {
                rev[cell] = c
            }
        }
        self.revealed = rev

        self.coins = max(0, s.coins)
        self.hintsLeft = max(0, s.hintsLeft)
        self.wordsTowardHint = max(0, min(s.wordsTowardHint, 9))
        self.lastPlayedEpochDay = max(0, s.lastPlayedEpochDay)
        self.currentStreak = max(0, s.currentStreak)
        self.lastSpinEpochDay = max(0, s.lastSpinEpochDay)
        self.selection = []
        // No persist() — restore() reads existing saved data.
    }
}
