package com.darkglass.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.darkglass.launcher.data.AppArt
import com.darkglass.launcher.data.UserSettings
import com.darkglass.launcher.model.Greeting
import com.darkglass.launcher.model.ResolvedSlot
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun HomeScreen(
    slots: List<ResolvedSlot>,
    arts: Map<String, AppArt>,
    settings: UserSettings,
    focusedIndex: Int,
    onFocusIndex: (Int) -> Unit,
    onLaunch: (ResolvedSlot) -> Unit,
    onMissing: (ResolvedSlot) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val intensity = glassIntensity(settings.glass)
    val animate = settings.animations

    // Relógio: atualiza a cada 10 s (suficiente para HH:mm, barato para a CPU da TV).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }

    // Entrada suave da tela.
    var entered by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(if (entered) 1f else 0f, focusSpec(animate, 650), label = "enter")
    val lift by animateFloatAsState(if (entered) 0f else 1f, focusSpec(animate, 650), label = "lift")

    // Foco inicial no dock (ou no último atalho usado).
    val requesters = remember(slots.size) { List(slots.size) { FocusRequester() } }
    LaunchedEffect(slots.size) {
        if (slots.isNotEmpty()) {
            withFrameNanos { }
            runCatching { requesters[focusedIndex.coerceIn(0, slots.lastIndex)].requestFocus() }
        }
    }

    // Margens seguras de overscan (~5% em 960×540 dp).
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp)
            .graphicsLayer { this.alpha = alpha },
    ) {
        TopBar(now, intensity, animate, onOpenSettings)
        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = lift * 24.dp.toPx() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            DText(
                Greeting.forHour(hour),
                46.sp,
                weight = FontWeight.Light,
                letterSpacing = (-0.5).sp,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            DText(
                "Seu entretenimento. Seu espaço.",
                17.sp,
                color = DG.Secondary,
                letterSpacing = 0.4.sp,
                align = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        Dock(slots, arts, intensity, animate, requesters, onFocusIndex, onLaunch, onMissing)
    }
}

@Composable
private fun TopBar(now: Long, intensity: Float, animate: Boolean, onOpenSettings: () -> Unit) {
    val date = Date(now)
    val time = remember(now / 60_000) { SimpleDateFormat("HH:mm", ptBR).format(date) }
    val day = remember(now / 60_000) {
        SimpleDateFormat("EEEE, d 'de' MMMM", ptBR).format(date).replaceFirstChar { it.titlecase(ptBR) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DText("DARK GLASS", 15.sp, color = DG.Frost, weight = FontWeight.SemiBold, letterSpacing = 4.sp)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            DText(time, 26.sp, weight = FontWeight.Light)
            DText(day, 13.sp, color = DG.Secondary)
        }
        Spacer(Modifier.width(18.dp))
        GlassButton("Ajustes", animate, onClick = onOpenSettings)
    }
}

@Composable
private fun Dock(
    slots: List<ResolvedSlot>,
    arts: Map<String, AppArt>,
    intensity: Float,
    animate: Boolean,
    requesters: List<FocusRequester>,
    onFocusIndex: (Int) -> Unit,
    onLaunch: (ResolvedSlot) -> Unit,
    onMissing: (ResolvedSlot) -> Unit,
) {
    if (slots.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gap = 16.dp
        val pad = 22.dp
        val n = slots.size
        val cardW = min((maxWidth - pad * 2 - gap * (n - 1)) / n, 150.dp)
        // O fundo de vidro fica numa camada separada para não recortar o zoom dos cartões.
        Box {
            Spacer(Modifier.matchParentSize().glassPanel(RoundedCornerShape(30.dp), intensity))
            Row(
                Modifier.padding(horizontal = pad, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                slots.forEachIndexed { i, slot ->
                    AppCard(
                        slot = slot,
                        art = slot.packageName?.let { arts[it] },
                        width = cardW,
                        intensity = intensity,
                        animate = animate,
                        modifier = Modifier.focusRequester(requesters[i]),
                        onFocused = { onFocusIndex(i) },
                        onClick = { if (slot.isInstalled) onLaunch(slot) else onMissing(slot) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard(
    slot: ResolvedSlot,
    art: AppArt?,
    width: Dp,
    intensity: Float,
    animate: Boolean,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.16f else 1f, focusSpec(animate, 190), label = "cardScale")
    val glow by animateFloatAsState(if (focused) 1f else 0f, focusSpec(animate, 220), label = "cardGlow")
    val shape = RoundedCornerShape(16.dp)
    val height = width * 9f / 16f

    Column(modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width, height)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .drawBehind {
                    // Halo luminoso simulado (camadas translúcidas, sem blur).
                    if (glow > 0f) {
                        for (step in 1..4) {
                            val spread = (step * 4).dp.toPx()
                            drawRoundRect(
                                color = DG.Ice.copy(alpha = 0.10f * glow / step),
                                topLeft = Offset(-spread, -spread),
                                size = Size(size.width + spread * 2, size.height + spread * 2),
                                cornerRadius = CornerRadius(16.dp.toPx() + spread),
                            )
                        }
                    }
                }
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .clip(shape)
                .background(DG.Graphite.copy(alpha = 0.55f + 0.25f * intensity))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) DG.Frost else Color.White.copy(alpha = 0.12f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                art != null && art.isBanner -> Image(
                    bitmap = art.bitmap,
                    contentDescription = slot.slot.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                art != null -> Image(
                    bitmap = art.bitmap,
                    contentDescription = slot.slot.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(height * 0.62f),
                )
                slot.isInstalled -> DText(slot.slot.title, 13.sp, weight = FontWeight.Medium)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DText("+", 22.sp, color = DG.Ice, weight = FontWeight.Light)
                    DText("Associar", 11.sp, color = DG.Secondary)
                }
            }
            // Reflexo de vidro sobre a arte.
            Spacer(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.10f + 0.10f * glow),
                            0.45f to Color.Transparent,
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(12.dp))
        DText(
            if (slot.isInstalled) slot.slot.title else "${slot.slot.title} · não instalado",
            12.sp,
            color = if (focused) DG.Frost else DG.Secondary,
            weight = if (focused) FontWeight.Medium else FontWeight.Normal,
            align = TextAlign.Center,
            maxLines = 1,
        )
    }
}
