package com.wordwheel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue

/**
 * Game state for a single level. Uses Compose state holders so UI reacts
 * automatically when state changes.
 */
class GameState(levelNum: Int, initialCoins: Int) {
    var level: Level = Level.get(levelNum)
        private set
    var levelNum: Int = levelNum
        private set

    val usedCells: Set<Pair<Int, Int>> = level.usedCells()
    val answers: Set<String> = level.answersSet()
    val bonusWords: Set<String> = level.bonusWords().toSet()

    val found = mutableStateListOf<String>()
    val bonusFound = mutableStateListOf<String>()
    val revealed = mutableStateMapOf<Pair<Int, Int>, Char>()

    var tiles by mutableStateOf(level.letters.toList())
        private set
    val selection = mutableStateListOf<Int>()

    var coins by mutableStateOf(initialCoins)
    var hintsLeft by mutableStateOf(5)
    var wordsTowardHint by mutableStateOf(0)

    fun currentWord(): String =
        selection.mapNotNull { idx -> tiles.getOrNull(idx) }.joinToString("")

    fun clearSelection() {
        selection.clear()
    }

    fun backspace() {
        if (selection.isNotEmpty()) selection.removeAt(selection.lastIndex)
    }

    fun shuffleTiles() {
        clearSelection()
        tiles = tiles.shuffled()
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
        return "Revealed a letter!"
    }
}
