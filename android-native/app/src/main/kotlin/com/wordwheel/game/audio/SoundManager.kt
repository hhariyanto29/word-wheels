package com.wordwheel.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.wordwheel.game.R

/**
 * Eight short SFX clips loaded into [SoundPool] for low-latency playback.
 * Volume follows the system media stream — there is no in-app mute toggle.
 *
 * To swap the synthesized placeholders for higher-quality clips later, drop
 * new files into res/raw/ using the same names (sfx_select.wav, etc.).
 */
enum class Sfx(val resId: Int) {
    Select(R.raw.sfx_select),
    WordFound(R.raw.sfx_word_found),
    Wrong(R.raw.sfx_wrong),
    Bonus(R.raw.sfx_bonus),
    Hint(R.raw.sfx_hint),
    Shuffle(R.raw.sfx_shuffle),
    Complete(R.raw.sfx_complete),
    Backspace(R.raw.sfx_backspace),
}

class SoundManager(context: Context) {
    private val pool: SoundPool
    private val ids = HashMap<Sfx, Int>(Sfx.entries.size)
    private val prefs = context.applicationContext
        .getSharedPreferences("word_wheel_audio", Context.MODE_PRIVATE)

    /** User-level toggle. When false, [play] is a no-op. Persisted across
     *  launches in a separate SharedPreferences so it isn't tied to the
     *  game-state schema. */
    var sfxEnabled: Boolean = prefs.getBoolean(KEY_SFX_ENABLED, true)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_SFX_ENABLED, value).apply()
        }

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        for (sfx in Sfx.entries) {
            ids[sfx] = pool.load(context, sfx.resId, 1)
        }
    }

    fun play(sfx: Sfx, volume: Float = 1.0f) {
        if (!sfxEnabled) return
        val id = ids[sfx] ?: return
        pool.play(id, volume, volume, 1, 0, 1.0f)
    }

    fun release() {
        pool.release()
        ids.clear()
    }

    private companion object {
        const val KEY_SFX_ENABLED = "sfx_enabled"
    }
}

/**
 * Composition-local handle so any composable can grab the SoundManager
 * without prop-drilling. Default no-op keeps previews working.
 */
val LocalSoundManager = staticCompositionLocalOf<SoundManager?> { null }
