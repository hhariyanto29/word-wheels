package com.wheelword.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.audio.SoundManager
import com.wheelword.game.theme.GameColors

/**
 * Settings panel — currently a single SFX toggle. Designed to grow as
 * we add more options (music, vibration, …) without restructuring the
 * dialog: each setting is a row inside the same column.
 */
@Composable
fun SettingsDialog(
    soundManager: SoundManager?,
    onDismiss: () -> Unit,
) {
    var sfxOn by remember { mutableStateOf(soundManager?.sfxEnabled ?: true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(GameColors.CompleteBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            SettingRow(
                title = "Sound effects",
                subtitle = "Plays a chime when a word lands.",
                checked = sfxOn,
                onCheckedChange = { v ->
                    sfxOn = v
                    soundManager?.sfxEnabled = v
                },
            )

            // Music slot (placeholder — re-enable when we add music tracks)
            // SettingRow(title = "Music", subtitle = "Background loop.", …)

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF50A0E6))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 36.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Done",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = Color(0xA0FFFFFF),
                fontSize = 13.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF32C850),
                uncheckedThumbColor = Color(0xFFCCCCCC),
                uncheckedTrackColor = Color(0xFF555555),
            ),
        )
    }
}
