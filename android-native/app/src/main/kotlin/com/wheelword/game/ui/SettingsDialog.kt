package com.wheelword.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.audio.SoundManager
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import com.wheelword.game.theme.NunitoFamily

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

    ModalShell(onDismiss = onDismiss, widthFraction = 0.82f) {
        Text(
            text = "Settings",
            color = Color.White,
            fontFamily = BalooFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))

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
        CandyButton(
            text = "Close",
            onClick = onDismiss,
            color = GameColors.Blue,
            colorDeep = GameColors.BlueDeep,
            fontSize = 17.sp,
            modifier = Modifier.fillMaxWidth(),
            verticalPadding = 11.dp,
        )
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
                fontFamily = BalooFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = GameColors.InkSoft,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GameColors.Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x2EFFFFFF),
            ),
        )
    }
}
