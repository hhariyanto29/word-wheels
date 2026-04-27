package com.wordwheel.game.storage

import android.content.Context
import android.util.Log
import com.wordwheel.game.Dir
import com.wordwheel.game.GameState
import com.wordwheel.game.Level
import org.json.JSONArray
import org.json.JSONObject

/**
 * Snapshot of everything that must survive an app kill/restart. Captured
 * synchronously from [GameState.snapshot] and restored via
 * [GameState.restore].
 *
 * Designed as a plain data class (not Compose state) so it can be safely
 * serialised. Uses [org.json] rather than kotlinx.serialization or Gson
 * to avoid pulling in another dependency for a few tiny fields.
 */
data class GameSnapshot(
    val levelNum: Int,
    val coins: Int,
    val hintsLeft: Int,
    val wordsTowardHint: Int,
    val tiles: List<Char>,
    val found: List<String>,
    val bonusFound: List<String>,
    val revealed: Map<Pair<Int, Int>, Char>,
    // ── Meta-progression (schema v2) ────────────────────────────────
    /** Local epoch day (LocalDate.toEpochDay) the player last opened the
     *  app while we ticked the streak. Zero when never recorded. */
    val lastPlayedEpochDay: Long = 0L,
    /** Consecutive-days-played counter; bumped when last play was
     *  exactly yesterday, reset to 1 on bigger gaps. */
    val currentStreak: Int = 0,
    /** Local epoch day the player last claimed the daily spin. Used to
     *  decide whether the spin button is enabled. */
    val lastSpinEpochDay: Long = 0L,
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("v", SCHEMA_VERSION)
        obj.put("levelNum", levelNum)
        obj.put("coins", coins)
        obj.put("hintsLeft", hintsLeft)
        obj.put("wordsTowardHint", wordsTowardHint)
        obj.put("tiles", tiles.joinToString(""))
        obj.put("found", JSONArray(found))
        obj.put("bonusFound", JSONArray(bonusFound))
        val rev = JSONArray()
        for ((pos, ch) in revealed) {
            val cell = JSONObject()
            cell.put("r", pos.first)
            cell.put("c", pos.second)
            cell.put("ch", ch.toString())
            rev.put(cell)
        }
        obj.put("revealed", rev)
        obj.put("lastPlayedEpochDay", lastPlayedEpochDay)
        obj.put("currentStreak", currentStreak)
        obj.put("lastSpinEpochDay", lastSpinEpochDay)
        return obj.toString()
    }

    companion object {
        const val SCHEMA_VERSION = 2

        fun fromJson(json: String?): GameSnapshot? {
            if (json.isNullOrEmpty()) return null
            return try {
                val obj = JSONObject(json)
                // Ignore snapshots from a future schema we don't understand.
                if (obj.optInt("v", 1) > SCHEMA_VERSION) return null

                val levelNum = obj.getInt("levelNum")
                    .coerceIn(1, Level.TOTAL_LEVELS)
                val tilesStr = obj.getString("tiles")
                val tiles = tilesStr.toCharArray().toList()

                val foundArr = obj.getJSONArray("found")
                val found = List(foundArr.length()) { foundArr.getString(it) }
                val bonusArr = obj.getJSONArray("bonusFound")
                val bonusFound = List(bonusArr.length()) { bonusArr.getString(it) }

                val revArr = obj.getJSONArray("revealed")
                val revealed = buildMap<Pair<Int, Int>, Char> {
                    for (i in 0 until revArr.length()) {
                        val cell = revArr.getJSONObject(i)
                        val r = cell.getInt("r")
                        val c = cell.getInt("c")
                        val ch = cell.getString("ch").firstOrNull() ?: continue
                        put(r to c, ch)
                    }
                }

                GameSnapshot(
                    levelNum = levelNum,
                    coins = obj.getInt("coins").coerceAtLeast(0),
                    hintsLeft = obj.getInt("hintsLeft").coerceAtLeast(0),
                    wordsTowardHint = obj.getInt("wordsTowardHint")
                        .coerceIn(0, 9),
                    tiles = tiles,
                    found = found,
                    bonusFound = bonusFound,
                    revealed = revealed,
                    // optInt → 0 default keeps v1 saves valid on upgrade
                    lastPlayedEpochDay = obj.optLong("lastPlayedEpochDay", 0L),
                    currentStreak = obj.optInt("currentStreak", 0).coerceAtLeast(0),
                    lastSpinEpochDay = obj.optLong("lastSpinEpochDay", 0L),
                )
            } catch (t: Throwable) {
                // Corrupt save — wipe it and start fresh rather than crash.
                Log.w(TAG, "Failed to parse game snapshot, discarding: ${t.message}")
                null
            }
        }
    }
}

/**
 * Persists game progress to [SharedPreferences]. Synchronous writes are
 * fine here — the JSON payload is <1 KB — but we also post to
 * `apply()` so we don't block the main thread on fsync.
 */
class GameStorage(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(snapshot: GameSnapshot) {
        prefs.edit()
            .putString(KEY_SNAPSHOT, snapshot.toJson())
            .apply()
    }

    fun load(): GameSnapshot? =
        GameSnapshot.fromJson(prefs.getString(KEY_SNAPSHOT, null))

    fun clear() {
        prefs.edit().remove(KEY_SNAPSHOT).apply()
    }

    companion object {
        private const val PREFS_NAME = "word_wheel_game"
        private const val KEY_SNAPSHOT = "snapshot"
    }
}

private const val TAG = "WordWheelStorage"
