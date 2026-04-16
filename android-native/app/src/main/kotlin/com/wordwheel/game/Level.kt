package com.wordwheel.game

enum class Dir { ACROSS, DOWN }

data class PlacedWord(
    val word: String,
    val row: Int,
    val col: Int,
    val dir: Dir,
)

data class Level(
    val rows: Int,
    val cols: Int,
    val letters: List<Char>,
    val words: List<PlacedWord>,
) {
    fun answersSet(): Set<String> = words.map { it.word }.toSet()

    fun usedCells(): Set<Pair<Int, Int>> {
        val used = mutableSetOf<Pair<Int, Int>>()
        for (pw in words) {
            for (i in pw.word.indices) {
                val (r, c) = when (pw.dir) {
                    Dir.ACROSS -> pw.row to (pw.col + i)
                    Dir.DOWN -> (pw.row + i) to pw.col
                }
                used.add(r to c)
            }
        }
        return used
    }

    fun solutionLetterAt(row: Int, col: Int): Char? {
        for (pw in words) {
            for ((i, ch) in pw.word.withIndex()) {
                val (r, c) = when (pw.dir) {
                    Dir.ACROSS -> pw.row to (pw.col + i)
                    Dir.DOWN -> (pw.row + i) to pw.col
                }
                if (r == row && c == col) return ch
            }
        }
        return null
    }

    fun bonusWords(): List<String> = when (letters) {
        listOf('C', 'A', 'T', 'S') -> listOf("ACT", "ACTS", "SCAT", "CATS")
        listOf('S', 'P', 'I', 'N', 'E') -> listOf("PIN", "PIE", "SIP", "SNIP", "PIES", "PENS", "SINE", "PINE")
        listOf('H', 'A', 'S', 'T', 'E') -> listOf("HEAT", "SET", "HAT", "TEA", "ATE", "ETA", "EAST", "EATS")
        listOf('W', 'A', 'R', 'M', 'S') -> listOf("WARMS", "WARS", "ARMS", "MARS", "ARM", "MAR", "RAW", "WAR")
        listOf('C', 'A', 'R', 'E', 'S') -> listOf("RACE", "CARS", "EARS", "ACE", "ARC", "SCAR", "ARCS", "ACRE", "ACES")
        listOf('G', 'R', 'I', 'N', 'D', 'S') -> listOf("GRINDS", "GRID", "RIND", "DING", "RING", "GRIN", "RINGS")
        listOf('P', 'L', 'A', 'N', 'E', 'T', 'S') -> listOf("PLAN", "PLANT", "LEAN", "PLATE", "STEAL", "LANE", "TALE", "PELT")
        listOf('C', 'R', 'A', 'N', 'E', 'S', 'D') -> listOf("DANCE", "SCARE", "RACED", "CEDAR", "CANE", "ACRE", "RAN")
        listOf('S', 'T', 'O', 'R', 'E', 'D') -> listOf("RODE", "DOER", "STORE", "DOTES", "TROD", "ODES", "SORE", "TORE")
        listOf('T', 'R', 'A', 'I', 'N', 'S', 'E') -> listOf("STARE", "RETAIN", "SATIRE", "INSERT", "STAIN", "RAIN", "RISE", "TIRE")
        else -> emptyList()
    }

    companion object {
        const val TOTAL_LEVELS = 10

        fun get(levelNum: Int): Level = when (levelNum) {
            1 -> Level(
                rows = 5, cols = 5,
                letters = listOf('C', 'A', 'T', 'S'),
                words = listOf(
                    PlacedWord("CAST", 1, 0, Dir.ACROSS),
                    PlacedWord("CAT", 1, 0, Dir.DOWN),
                    PlacedWord("SAT", 1, 2, Dir.DOWN),
                ),
            )
            2 -> Level(
                rows = 6, cols = 6,
                letters = listOf('S', 'P', 'I', 'N', 'E'),
                words = listOf(
                    PlacedWord("SPINE", 1, 0, Dir.ACROSS),
                    PlacedWord("SIN", 1, 0, Dir.DOWN),
                    PlacedWord("NIP", 1, 3, Dir.DOWN),
                    PlacedWord("PEN", 3, 3, Dir.ACROSS),
                ),
            )
            3 -> Level(
                rows = 7, cols = 7,
                letters = listOf('H', 'A', 'S', 'T', 'E'),
                words = listOf(
                    PlacedWord("HASTE", 1, 0, Dir.ACROSS),
                    PlacedWord("HATE", 1, 0, Dir.DOWN),
                    PlacedWord("SEAT", 1, 2, Dir.DOWN),
                    PlacedWord("EAT", 4, 0, Dir.ACROSS),
                ),
            )
            4 -> Level(
                rows = 7, cols = 7,
                letters = listOf('W', 'A', 'R', 'M', 'S'),
                words = listOf(
                    PlacedWord("SWARM", 1, 0, Dir.ACROSS),
                    PlacedWord("SAW", 1, 0, Dir.DOWN),
                    PlacedWord("RAM", 1, 3, Dir.DOWN),
                    PlacedWord("WARM", 3, 0, Dir.ACROSS),
                ),
            )
            5 -> Level(
                rows = 7, cols = 7,
                letters = listOf('C', 'A', 'R', 'E', 'S'),
                words = listOf(
                    PlacedWord("CARES", 1, 0, Dir.ACROSS),
                    PlacedWord("CARE", 1, 0, Dir.DOWN),
                    PlacedWord("SEAR", 1, 4, Dir.DOWN),
                    PlacedWord("ERA", 4, 0, Dir.ACROSS),
                ),
            )
            6 -> Level(
                rows = 7, cols = 7,
                letters = listOf('G', 'R', 'I', 'N', 'D', 'S'),
                words = listOf(
                    PlacedWord("GRINS", 1, 0, Dir.ACROSS),
                    PlacedWord("GIN", 1, 0, Dir.DOWN),
                    PlacedWord("SIR", 1, 4, Dir.DOWN),
                    PlacedWord("RID", 3, 4, Dir.ACROSS),
                    PlacedWord("DIG", 3, 6, Dir.DOWN),
                ),
            )
            7 -> Level(
                rows = 8, cols = 8,
                letters = listOf('P', 'L', 'A', 'N', 'E', 'T', 'S'),
                words = listOf(
                    PlacedWord("PLANETS", 1, 0, Dir.ACROSS),
                    PlacedWord("PETAL", 1, 0, Dir.DOWN),
                    PlacedWord("NETS", 1, 3, Dir.DOWN),
                    PlacedWord("SLANT", 1, 6, Dir.DOWN),
                    PlacedWord("ANTS", 4, 0, Dir.ACROSS),
                ),
            )
            8 -> Level(
                rows = 8, cols = 8,
                letters = listOf('C', 'R', 'A', 'N', 'E', 'S', 'D'),
                words = listOf(
                    PlacedWord("CRANES", 1, 0, Dir.ACROSS),
                    PlacedWord("CARED", 1, 0, Dir.DOWN),
                    PlacedWord("NEARS", 1, 3, Dir.DOWN),
                    PlacedWord("SANE", 1, 5, Dir.DOWN),
                    PlacedWord("DENS", 5, 0, Dir.ACROSS),
                    PlacedWord("END", 5, 1, Dir.DOWN),
                ),
            )
            9 -> Level(
                rows = 8, cols = 8,
                letters = listOf('S', 'T', 'O', 'R', 'E', 'D'),
                words = listOf(
                    PlacedWord("STORED", 1, 0, Dir.ACROSS),
                    PlacedWord("SORT", 1, 0, Dir.DOWN),
                    PlacedWord("REST", 1, 3, Dir.DOWN),
                    PlacedWord("DOSE", 1, 5, Dir.DOWN),
                    PlacedWord("ROES", 3, 0, Dir.ACROSS),
                ),
            )
            else -> Level(
                rows = 9, cols = 9,
                letters = listOf('T', 'R', 'A', 'I', 'N', 'S', 'E'),
                words = listOf(
                    PlacedWord("TRAINS", 1, 0, Dir.ACROSS),
                    PlacedWord("TEARS", 1, 0, Dir.DOWN),
                    PlacedWord("INSET", 1, 3, Dir.DOWN),
                    PlacedWord("SIREN", 1, 5, Dir.DOWN),
                    PlacedWord("ANTS", 3, 0, Dir.ACROSS),
                    PlacedWord("SENT", 5, 0, Dir.ACROSS),
                ),
            )
        }
    }
}
