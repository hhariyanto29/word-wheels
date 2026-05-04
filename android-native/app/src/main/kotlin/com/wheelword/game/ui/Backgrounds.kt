package com.wheelword.game.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.wheelword.game.R

/**
 * Per-level country background. One image per 10-level range — see the
 * map below. Files live in the canonical `assets/backgrounds/` folder
 * at the repo root, picked up by Android via the `assets.srcDirs(...)`
 * alias in app/build.gradle.kts (so we ship one copy, not two).
 *
 * Returns the asset path to load, or null when the level is outside
 * the curated 1..100 range.
 */
private val BACKGROUND_BY_RANGE = arrayOf(
    "backgrounds/bg_lv_001_010_vietnam.jpg",
    "backgrounds/bg_lv_011_020_brunei.jpg",
    "backgrounds/bg_lv_021_030_malaysia.jpg",
    "backgrounds/bg_lv_031_040_myanmar.jpg",
    "backgrounds/bg_lv_041_050_papua_nugini.jpg",
    "backgrounds/bg_lv_051_060_filipina.jpg",
    "backgrounds/bg_lv_061_070_singapore.jpg",
    "backgrounds/bg_lv_071_080_thailand.jpg",
    "backgrounds/bg_lv_081_090_indonesia.jpg",
    "backgrounds/bg_lv_091_100_bali.jpg",
)

private fun backgroundAssetPath(level: Int): String? {
    val idx = (level - 1) / 10
    return BACKGROUND_BY_RANGE.getOrNull(idx)
}

/**
 * Renders the appropriate per-level background. Falls back to the
 * legacy `R.drawable.game_background` (Egypt) when [level] is outside
 * 1..100 — that drawable still ships as the spare default.
 *
 * Decoding from the asset stream is wrapped in `remember(level)` so
 * we don't re-decode on every recomposition; only when the level
 * actually changes.
 */
@Composable
fun GameBackgroundImage(
    level: Int,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val assetPath = backgroundAssetPath(level)
    if (assetPath == null) {
        Image(
            painter = painterResource(R.drawable.game_background),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        return
    }
    val context = LocalContext.current
    val imageBitmap = remember(assetPath) {
        context.assets.open(assetPath).use {
            BitmapFactory.decodeStream(it).asImageBitmap()
        }
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}
