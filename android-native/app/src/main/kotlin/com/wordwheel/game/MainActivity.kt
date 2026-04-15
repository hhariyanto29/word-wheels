package com.wordwheel.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wordwheel.game.audio.LocalSoundManager
import com.wordwheel.game.audio.SoundManager
import com.wordwheel.game.theme.WordWheelTheme
import com.wordwheel.game.ui.GameScreen

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Route the volume rocker to the media stream so SoundPool playback
        // (USAGE_GAME) responds to the user's media volume rather than ringer.
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        soundManager = SoundManager(applicationContext)
        enableEdgeToEdge()
        setContent {
            WordWheelTheme {
                CompositionLocalProvider(LocalSoundManager provides soundManager) {
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
