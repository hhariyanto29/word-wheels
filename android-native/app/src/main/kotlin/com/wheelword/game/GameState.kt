package com.wheelword.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.wheelword.game.storage.GameSnapshot

enum class AttemptResult { GRID, BONUS, DUPLICATE, INVALID, TOO_SHORT }

data class Attempt(val word: String, val result: AttemptResult)

private const val RECENT_ATTEMPTS_CAP = 5

/**
 * Cap on how many *dictionary-only* bonus words earn coins per level.
 * Curated `bonusWords` from level data are always rewarded; this only
 * limits the open-ended dictionary fallback to keep hint earn rate
 * tractable.
 *
 * 50 is a starting value. Tune from playtesting: too low and engaged
 * players hit the cap mid-level and feel cheated; too high and hints
 * become free.
 */
private const val DICT_BONUS_COIN_CAP = 50

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
    // Newest attempt first. Capped at RECENT_ATTEMPTS_CAP — only the
    // top few are rendered, but a small buffer leaves room to show more
    // if the design ever changes.
    val recentAttempts = mutableStateListOf<Attempt>()

    var tiles by mutableStateOf(level.letters.toList())
        private set
    val selection = mutableStateListOf<Int>()

    var coins by mutableStateOf(initialCoins)
    var hintsLeft by mutableStateOf(initialHints)
    var wordsTowardHint by mutableStateOf(initialWordsTowardHint)
    /// Counter of dictionary-only bonus words that earned coins this level.
    /// Reset on level transition. Capped at [DICT_BONUS_COIN_CAP] — bonuses
    /// found beyond it are still recognised but don't progress hints/coins.
    var dictBonusEarned by mutableStateOf(0)
        private set

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
        if (guess.length < 2) {
            recordAttempt(guess, AttemptResult.TOO_SHORT)
            return "Make a longer word."
        }

        if (answers.contains(guess)) {
            return if (!found.contains(guess)) {
                found.add(guess)
                coins += 2
                val hintMsg = awardProgress()
                clearSelection()
                recordAttempt(guess, AttemptResult.GRID)
                persist()
                "Found: $guess (+2 pts)$hintMsg"
            } else {
                clearSelection()
                recordAttempt(guess, AttemptResult.DUPLICATE)
                "Already found."
            }
        }

        if (bonusWords.contains(guess)) {
            return if (!bonusFound.contains(guess)) {
                bonusFound.add(guess)
                coins += 2
                val hintMsg = awardProgress()
                clearSelection()
                recordAttempt(guess, AttemptResult.BONUS)
                persist()
                "Bonus: $guess (+2 pts)$hintMsg"
            } else {
                clearSelection()
                recordAttempt(guess, AttemptResult.DUPLICATE)
                "Bonus already counted."
            }
        }

        // Dictionary fallback (Option C hybrid). The wheel selection
        // mechanism guarantees the word is spellable from the wheel, so
        // we only need a dictionary membership check. First N earn
        // coins/hint progress; the rest are recognised but gated to
        // preserve hint pacing.
        if (Dictionary.contains(guess)) {
            return if (!bonusFound.contains(guess)) {
                bonusFound.add(guess)
                if (dictBonusEarned < DICT_BONUS_COIN_CAP) {
                    coins += 2
                    dictBonusEarned += 1
                    val hintMsg = awardProgress()
                    clearSelection()
                    recordAttempt(guess, AttemptResult.BONUS)
                    persist()
                    "Bonus: $guess (+2 pts)$hintMsg"
                } else {
                    clearSelection()
                    recordAttempt(guess, AttemptResult.BONUS)
                    persist()
                    "Bonus: $guess (cap reached)"
                }
            } else {
                clearSelection()
                recordAttempt(guess, AttemptResult.DUPLICATE)
                "Bonus already counted."
            }
        }

        clearSelection()
        recordAttempt(guess, AttemptResult.INVALID)
        return "No match."
    }

    private fun recordAttempt(word: String, result: AttemptResult) {
        // Skip empty (auto-submit on a tap with no selection) and the
        // TOO_SHORT case for sub-2-letter prefixes — they're noise the
        // player didn't intend to "try".
        if (word.length < 2) return
        recentAttempts.add(0, Attempt(word, result))
        while (recentAttempts.size > RECENT_ATTEMPTS_CAP) {
            recentAttempts.removeAt(recentAttempts.size - 1)
        }
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
        recentAttempts.clear()
        dictBonusEarned = 0
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
