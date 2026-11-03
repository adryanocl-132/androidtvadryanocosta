package com.darkglass.launcher.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta oficial Dark Glass. */
object DG {
    val Obsidian = Color(0xFF080B12)
    val Graphite = Color(0xFF1C2535)
    val Ice = Color(0xFFA7C7FF)
    val Frost = Color(0xFFEAF1FA)
    val Secondary = Color(0xFFA8B8D2)
}

data class Glow(val x: Float, val y: Float, val radius: Float, val color: Color)
data class Wallpaper(val name: String, val base: List<Color>, val glows: List<Glow>)

/** Papéis de parede gerados (sem imagens, sem permissão de armazenamento). */
val Wallpapers = listOf(
    Wallpaper(
        "Aurora Gelo", listOf(Color(0xFF080B12), Color(0xFF0C1322)),
        listOf(Glow(0.12f, 0.08f, 0.55f, Color(0x7A2A4A7F)), Glow(0.88f, 0.95f, 0.6f, Color(0x2EA7C7FF))),
    ),
    Wallpaper(
        "Nebulosa", listOf(Color(0xFF0A0812), Color(0xFF120E22)),
        listOf(Glow(0.8f, 0.1f, 0.55f, Color(0x664B3A8F)), Glow(0.15f, 0.9f, 0.55f, Color(0x2E7FA7FF))),
    ),
    Wallpaper(
        "Grafite", listOf(Color(0xFF0B0E14), Color(0xFF151B27)),
        listOf(Glow(0.5f, 0.0f, 0.7f, Color(0x401C2535)), Glow(0.5f, 1.1f, 0.6f, Color(0x24EAF1FA))),
    ),
    Wallpaper(
        "Oceano Profundo", listOf(Color(0xFF060C12), Color(0xFF0A1820)),
        listOf(Glow(0.2f, 0.2f, 0.6f, Color(0x5C1E5A6E)), Glow(0.9f, 0.8f, 0.5f, Color(0x2EA7E1FF))),
    ),
)

val GlassLevels = listOf("Suave", "Médio", "Intenso")
fun glassIntensity(level: Int): Float = when (level) { 0 -> 0.5f; 2 -> 1f; else -> 0.75f }

/** Fundo com gradientes e luz ambiente estáticos (zero custo por frame). */
@Composable
fun WallpaperBackground(index: Int) {
    val wp = Wallpapers[index.coerceIn(0, Wallpapers.lastIndex)]
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(wp.base))
        val m = maxOf(size.width, size.height)
        wp.glows.forEach { g ->
            drawRect(
                Brush.radialGradient(
                    listOf(g.color, Color.Transparent),
                    center = Offset(size.width * g.x, size.height * g.y),
                    radius = m * g.radius,
                ),
            )
        }
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                center = center,
                radius = m * 0.75f,
            ),
        )
    }
}

/**
 * Painel de vidro: preenchimento translúcido + borda luminosa + reflexo superior.
 * Vidro "simulado" por gradientes — funciona em qualquer GPU, sem RenderEffect/blur.
 */
fun Modifier.glassPanel(shape: Shape, intensity: Float): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.03f + 0.07f * intensity),
                DG.Graphite.copy(alpha = 0.30f + 0.30f * intensity),
            ),
        ),
    )
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f * intensity),
                1f to Color.Transparent,
                endY = size.height * 0.45f,
            ),
            size = Size(size.width, size.height * 0.45f),
        )
    }
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.10f + 0.28f * intensity), DG.Ice.copy(alpha = 0.05f)),
        ),
        shape,
    )

fun focusSpec(animate: Boolean, millis: Int = 170): AnimationSpec<Float> =
    if (animate) tween(millis) else snap()

@Composable
fun DText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = DG.Frost,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            textAlign = align,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Botão de vidro navegável por controle remoto (setas + OK). */
@Composable
fun GlassButton(
    text: String,
    animate: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, focusSpec(animate), label = "btnScale")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .clip(shape)
            .background(
                when {
                    focused -> DG.Frost
                    selected -> DG.Ice.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.06f)
                },
            )
            .border(
                1.dp,
                when {
                    focused -> DG.Frost
                    selected -> DG.Ice.copy(alpha = 0.75f)
                    else -> Color.White.copy(alpha = 0.12f)
                },
                shape,
            )
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        DText(
            text,
            15.sp,
            color = if (focused) DG.Obsidian else DG.Frost,
            weight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
