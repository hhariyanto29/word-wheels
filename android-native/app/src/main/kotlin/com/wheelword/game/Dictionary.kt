package com.wheelword.game

import android.content.Context

/**
 * In-memory word dictionary used to recognise bonus words beyond the
 * per-level curated `bonusWords` list. Source file ships as an asset
 * (assets/dictionary.txt) — see tools/dictionary/build_dictionary.py
 * for how it's produced.
 *
 * Loaded once at app start (see [load]). Until [isLoaded] flips true,
 * [contains] returns false — that means dictionary-recognised words
 * count as "no match" during the ~100-300 ms load window. Acceptable
 * because the wheel selection takes longer than that to produce a word
 * in practice.
 */
object Dictionary {
    @Volatile private var words: Set<String>? = null

    val isLoaded: Boolean get() = words != null
    val size: Int get() = words?.size ?: 0

    /**
     * Synchronously read assets/dictionary.txt into a HashSet. Call from
     * a background coroutine (Dispatchers.IO) — reading 200k+ lines on
     * the main thread will jank app startup.
     */
    fun load(context: Context) {
        if (words != null) return
        // Pre-size to avoid rehashing during the main load. ~230k words
        // at 0.75 load factor → 320k buckets.
        val set = HashSet<String>(320_000)
        context.applicationContext.assets.open("dictionary.txt")
            .bufferedReader()
            .use { reader ->
                reader.lineSequence().forEach { line ->
                    val w = line.trim()
                    if (w.isNotEmpty()) set.add(w)
                }
            }
        words = set
    }

    fun contains(word: String): Boolean {
        val w = word.lowercase()
        return words?.contains(w) ?: false
    }
}
