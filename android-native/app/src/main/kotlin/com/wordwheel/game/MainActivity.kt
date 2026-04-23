package com.wordwheel.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wordwheel.game.audio.LocalSoundManager
import com.wordwheel.game.audio.SoundManager
import com.wordwheel.game.storage.GameStorage
import com.wordwheel.game.theme.WordWheelTheme
import com.wordwheel.game.ui.GameScreen

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
