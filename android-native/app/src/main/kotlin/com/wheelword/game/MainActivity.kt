package com.wheelword.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.wheelword.game.audio.LocalSoundManager
import com.wheelword.game.audio.SoundManager
import com.wheelword.game.storage.GameStorage
import com.wheelword.game.theme.WordWheelTheme
import com.wheelword.game.ui.GameScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalGameStorage = staticCompositionLocalOf<GameStorage?> { null }

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager
    private lateinit var storage: GameStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Route the volume rocker to the media stream so SoundPool playback
        // (USAGE_GAME) responds to the user's media volume rather than ringer.
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        soundManager = SoundManager(applicationContext)
        storage = GameStorage(applicationContext)
        // Load the bonus-word dictionary on a background thread so the
        // ~200 ms file read doesn't delay the first frame. Until it
        // finishes, dictionary-recognised words won't count — which is
        // fine because the splash + initial render take longer than that.
        lifecycleScope.launch(Dispatchers.IO) {
            Dictionary.load(applicationContext)
        }
        enableEdgeToEdge()
        setContent {
            WordWheelTheme {
                CompositionLocalProvider(
                    LocalSoundManager provides soundManager,
                    LocalGameStorage provides storage,
                ) {
                    GameScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}
