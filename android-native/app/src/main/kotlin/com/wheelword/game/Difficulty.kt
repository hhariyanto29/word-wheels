package com.wheelword.game

/**
 * Difficulty buckets shown to the player at level milestones (10, 20,
 * 30 …). The text + accent colour bubble pops via [DifficultyDialog]
 * after the corresponding level finishes, signalling the next tier.
 *
 * This is a pure presentation thing — gameplay difficulty is already
 * dictated by the procedurally generated puzzle complexity per level.
 */
enum class Difficulty(
    val displayName: String,
    val tagline: String,
    val accentHex: Long,
) {
    EASY(    "Easy",       "A gentle warm-up.",                   0xFF32C850),
    MEDIUM(  "Medium",     "Things start mixing up.",             0xFF50A0E6),
    HARD(    "Hard",       "Crosswords get bigger.",              0xFFFFB400),
    EXPERT(  "Expert",     "Multi-word grids and rare letters.",  0xFFE65050),
    MASTER(  "Master",     "8-letter spines from here on.",       0xFFB450E6),
    LEGEND(  "Legend",     "Push through to 100.",                0xFFFFDC50),
    ;
    companion object {
        /**
         * Tier the player just unlocked by completing [completedLevel].
         * Returns null when no milestone fires (i.e. levels 1–9, 11–19, …).
         */
        fun milestoneFor(completedLevel: Int): Difficulty? = when (completedLevel) {
            10 -> MEDIUM
            20 -> HARD
            40 -> EXPERT
            60 -> MASTER
            80 -> LEGEND
            else -> null
        }
    }
}
