package com.wheelword.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelword.game.theme.BalooFamily
import com.wheelword.game.theme.GameColors
import kotlin.math.cos
import kotlin.math.sin

/*
 * Word Wheel v2 design kit — shared building blocks from the design
 * handoff (game/ui.jsx): glass HUD pills, the star coin, candy
 * buttons, round glass icon buttons, and the navy modal shell.
 */

/** Gold coin with a white star — the game's currency glyph. */
@Composable
fun StarCoinIcon(size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = GameColors.Gold, radius = r, center = c)
        drawCircle(
            color = GameColors.GoldDeep,
            radius = r - r * 0.07f,
            center = c,
            style = Stroke(width = r * 0.14f),
        )
        drawCircle(
            color = Color(0x80FFFFFF),
            radius = r * 0.74f,
            center = c,
            style = Stroke(width = r * 0.09f),
        )
        // Five-point star, point-up.
        val outer = r * 0.58f
        val inner = r * 0.24f
        val star = Path()
        for (i in 0 until 10) {
            val ang = (i * 36f - 90f) * (Math.PI.toFloat() / 180f)
            val rad = if (i % 2 == 0) outer else inner
            val x = c.x + cos(ang) * rad
            val y = c.y + sin(ang) * rad
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, Color.White)
    }
}

/** Glass capsule for HUD stats (coins, streak, word count, level). */
@Composable
fun HUDPill(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(GameColors.Glass)
            .border(width = 1.dp, color = GameColors.Stroke, shape = RoundedCornerShape(18.dp))
            .run { if (onClick != null) clickable(onClick = onClick) else this }
            .padding(start = 8.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Text style used inside HUD pills. */
val HUDPillTextStyle = TextStyle(
    fontFamily = BalooFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    color = Color.White,
)

/** Round glass icon button (settings cog, help "?"). */
@Composable
fun RoundGlassButton(
    onClick: () -> Unit,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = CircleShape)
            .size(size)
            .clip(CircleShape)
            .background(GameColors.Glass)
            .border(width = 1.dp, color = GameColors.Stroke, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * "Candy" pill button: vertical colour gradient + glossy top highlight
 * + dark bottom lip, chunky Baloo label with a soft text shadow.
 */
@Composable
fun CandyButton(
    text: String,
    onClick: () -> Unit,
    color: Color = GameColors.Green,
    colorDeep: Color = GameColors.GreenDeep,
    textColor: Color = Color.White,
    fontSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 40.dp,
    verticalPadding: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(colors = listOf(color, colorDeep)))
            // Glossy top edge + darker bottom lip, like the prototype's
            // inset box-shadows.
            .background(
                Brush.verticalGradient(
                    0f to Color(0x47FFFFFF),
                    0.28f to Color.Transparent,
                    0.82f to Color.Transparent,
                    1f to Color(0x2E000000),
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = BalooFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = 1.sp,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0x40000000),
                    offset = Offset(0f, 4f),
                    blurRadius = 4f,
                ),
            ),
        )
    }
}

/**
 * Navy gradient modal panel (the design's WWModal): dim scrim behind,
 * rounded-28 panel with a soft border. Tap outside to dismiss when
 * [onDismiss] is non-null.
 */
@Composable
fun ModalShell(
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.86f,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.Scrim)
            .clickable(enabled = onDismiss != null) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(widthFraction)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GameColors.PanelTop, GameColors.PanelSolid),
                    )
                )
                .border(width = 1.5.dp, color = GameColors.Stroke, shape = RoundedCornerShape(28.dp))
                // Consume clicks so taps inside the panel don't dismiss.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                // Scroll when content is taller than the screen (small
                // phones in landscape, the Help dialog, …).
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
