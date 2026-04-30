package com.wheelword.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.wheelword.game.storage.GameSnapshot

/**
 * Game state for a single level. Uses Compose state holders so UI reacts
 * automatically when state changes.
 *
 * Every state-mutating method calls [persist] at the end so progress
 * survives an app kill/restart. The [persist] callback is supplied by the
 * caller — typically a `GameStorage.save(snapshot())` closure from
 * `GameScreen`.
 */
class GameState(
    levelNum: Int,
    initialCoins: Int,
    initialHints: Int = 5,
    initialWordsTowardHint: Int = 0,
    private val persist: () -> Unit = {},
) {
    var level: Level = Level.get(levelNum)
        private set
    var levelNum: Int = levelNum
        private set

    // Derived structural data — recomputed whenever `level` changes.
    var usedCells: Set<Pair<Int, Int>> = level.usedCells()
        private set
    var answers: Set<String> = level.answersSet()
        private set
    var bonusWords: Set<String> = level.bonusWords().toSet()
        private set

    val found = mutableStateListOf<String>()
    val bonusFound = mutableStateListOf<String>()
    val revealed = mutableStateMapOf<Pair<Int, Int>, Char>()

    var tiles by mutableStateOf(level.letters.toList())
        private set
    val selection = mutableStateListOf<Int>()

    var coins by mutableStateOf(initialCoins)
    var hintsLeft by mutableStateOf(initialHints)
    var wordsTowardHint by mutableStateOf(initialWordsTowardHint)

    // ── Meta-progression (streak + daily spin) ──────────────────────
    var lastPlayedEpochDay by mutableStateOf(0L)
    var currentStreak by mutableStateOf(0)
    var lastSpinEpochDay by mutableStateOf(0L)

    fun currentWord(): String =
        selection.mapNotNull { idx -> tiles.getOrNull(idx) }.joinToString("")

    fun clearSelection() {
        selection.clear()
        // No persist — selection is transient within a turn.
    }

    fun backspace() {
        if (selection.isNotEmpty()) selection.removeAt(selection.lastIndex)
    }

    fun shuffleTiles() {
        clearSelection()
        tiles = tiles.shuffled()
        persist()
    }

    fun isComplete(): Boolean = found.size == answers.size

    fun visibleLettersMap(): Map<Pair<Int, Int>, Char> {
        val map = HashMap<Pair<Int, Int>, Char>(revealed)
        for (pw in level.words) {
            if (found.contains(pw.word)) {
                for ((i, ch) in pw.word.withIndex()) {
                    val pos = when (pw.dir) {
                        Dir.ACROSS -> pw.row to (pw.col + i)
                        Dir.DOWN -> (pw.row + i) to pw.col
                    }
                    map[pos] = ch
                }
            }
        }
        return map
    }

    /** Returns a status message after submission. */
    fun trySubmit(): String {
        val guess = currentWord()
        if (guess.length < 2) return "Make a longer word."

        if (answers.contains(guess)) {
            return if (!found.contains(guess)) {
                found.add(guess)
                coins += 2
                val hintMsg = awardProgress()
                clearSelection()
                persist()
                "Found: $guess (+2 pts)$hintMsg"
            } else {
                clearSelection()
                "Already found."
            }
        }

        if (bonusWords.contains(guess)) {
            return if (!bonusFound.contains(guess)) {
                bonusFound.add(guess)
                coins += 2
                val hintMsg = awardProgress()
                clearSelection()
                persist()
                "Bonus: $guess (+2 pts)$hintMsg"
            } else {
                clearSelection()
                "Bonus already counted."
            }
        }

        clearSelection()
        return "No match."
    }

    private fun awardProgress(): String {
        wordsTowardHint += 1
        return if (wordsTowardHint >= 10) {
            wordsTowardHint = 0
            hintsLeft += 1
            " +1 hint!"
        } else ""
    }

    fun hintRevealRandomLetter(): String {
        if (hintsLeft == 0) return "No hints left! Find more words to earn hints."

        val visible = visibleLettersMap()
        val candidates = usedCells.filter { !visible.containsKey(it) }
        if (candidates.isEmpty()) return "No letters left to reveal."

        val (r, c) = candidates.random()
        val ch = level.solutionLetterAt(r, c) ?: return "Hint failed."
        revealed[r to c] = ch
        hintsLeft -= 1
        persist()
        return "Revealed a letter!"
    }

    /**
     * Switch to a new level. Tile order and progress reset; coins/hints
     * carry over (supplied by the caller).
     */
    fun goToLevel(newLevel: Int) {
        val lvl = Level.get(newLevel)
        level = lvl
        levelNum = newLevel
        usedCells = lvl.usedCells()
        answers = lvl.answersSet()
        bonusWords = lvl.bonusWords().toSet()
        tiles = lvl.letters.toList()
        found.clear()
        bonusFound.clear()
        revealed.clear()
        selection.clear()
        persist()
    }

    /** Snapshot of everything that should survive an app restart. */
    fun snapshot(): GameSnapshot = GameSnapshot(
        levelNum = levelNum,
        coins = coins,
        hintsLeft = hintsLeft,
        wordsTowardHint = wordsTowardHint,
        tiles = tiles,
        found = found.toList(),
        bonusFound = bonusFound.toList(),
        revealed = revealed.toMap(),
        lastPlayedEpochDay = lastPlayedEpochDay,
        currentStreak = currentStreak,
        lastSpinEpochDay = lastSpinEpochDay,
    )

    // ── Streak ────────────────────────────────────────────────────
    /**
     * Call once on app open. Bumps the streak if exactly one day has
     * passed since the last play, resets to 1 on bigger gaps, and
     * is a no-op if the player already opened the app today.
     */
    fun tickDailyStreak(today: Long) {
        if (lastPlayedEpochDay == today) return
        currentStreak = when {
            lastPlayedEpochDay == 0L -> 1
            today - lastPlayedEpochDay == 1L -> currentStreak + 1
            else -> 1  // missed a day — reset
        }
        lastPlayedEpochDay = today
        persist()
    }

    // ── Daily spin ────────────────────────────────────────────────
    fun canSpinToday(today: Long): Boolean = lastSpinEpochDay != today

    /**
     * Apply a spin reward and mark today as claimed. Caller picked the
     * reward via the wheel UI; this just records the result.
     */
    fun applySpinReward(today: Long, coinsAdded: Int = 0, hintsAdded: Int = 0) {
        coins += coinsAdded
        hintsLeft += hintsAdded
        lastSpinEpochDay = today
        persist()
    }

    /**
     * Rehydrate from a previously-saved snapshot. Defensive — if the saved
     * tile multiset doesn't match the current level's letters (e.g. level
     * data was edited in an update), fall back to the level's original
     * order. Likewise, only restore `found` / `bonusFound` words that are
     * still valid for this level.
     */
    fun restore(s: GameSnapshot) {
        goToLevel(s.levelNum)  // realigns level-derived fields

        val savedMultiset = s.tiles.sorted()
        val levelMultiset = level.letters.sorted()
        tiles = if (savedMultiset == levelMultiset) s.tiles else level.letters.toList()

        found.clear()
        found.addAll(s.found.filter { it in answers })

        bonusFound.clear()
        bonusFound.addAll(s.bonusFound.filter { it in bonusWords })

        revealed.clear()
        for ((pos, ch) in s.revealed) {
            if (pos in usedCells) revealed[pos] = ch
        }

        coins = s.coins
        hintsLeft = s.hintsLeft
        wordsTowardHint = s.wordsTowardHint
        lastPlayedEpochDay = s.lastPlayedEpochDay
        currentStreak = s.currentStreak
        lastSpinEpochDay = s.lastSpinEpochDay
        // No persist() here — restore is reading existing saved data.
    }
}
