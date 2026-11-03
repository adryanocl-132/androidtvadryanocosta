package com.darkglass.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkglass.launcher.BuildConfig
import com.darkglass.launcher.data.UserSettings
import com.darkglass.launcher.model.InstalledApp
import com.darkglass.launcher.model.ResolvedSlot

@Composable
fun SettingsScreen(
    settings: UserSettings,
    slots: List<ResolvedSlot>,
    homeCandidateEnabled: Boolean,
    autostartEnabled: Boolean,
    canShowOnStartup: Boolean,
    onToggleAutostart: (Boolean) -> Unit,
    onGrantStartupPermission: () -> Unit,
    onChange: ((UserSettings) -> UserSettings) -> Unit,
    onAssociate: (slotId: String) -> Unit,
    onToggleHomeCandidate: (Boolean) -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onExitToOriginal: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val animate = settings.animations
    val intensity = glassIntensity(settings.glass)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight()
                .glassPanel(RoundedCornerShape(28.dp), intensity)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DText("Ajustes do Dark Glass", 26.sp, weight = FontWeight.Light)
                Spacer(Modifier.weight(1f))
                GlassButton("Voltar", animate, Modifier.focusRequester(first), onClick = onBack)
            }

            Section("Papel de parede")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Wallpapers.forEachIndexed { i, wp ->
                    GlassButton(wp.name, animate, selected = settings.wallpaper == i) {
                        onChange { it.copy(wallpaper = i) }
                    }
                }
            }

            Section("Intensidade do vidro")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassLevels.forEachIndexed { i, name ->
                    GlassButton(name, animate, selected = settings.glass == i) {
                        onChange { it.copy(glass = i) }
                    }
                }
            }

            Section("Animações")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Ativadas", animate, selected = settings.animations) {
                    onChange { it.copy(animations = true) }
                }
                GlassButton("Desativadas", animate, selected = !settings.animations) {
                    onChange { it.copy(animations = false) }
                }
            }

            Section("Favoritos — ordem e associação")
            slots.forEachIndexed { index, s ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DText("${index + 1}. ${s.slot.title}", 16.sp, Modifier.width(170.dp), maxLines = 1)
                    DText(
                        when {
                            s.packageName == null -> "Não encontrado"
                            else -> "${s.appLabel ?: ""} · ${s.packageName}"
                        },
                        12.sp,
                        Modifier.weight(1f).padding(end = 12.dp),
                        color = DG.Secondary,
                        maxLines = 1,
                    )
                    GlassButton("Subir", animate) {
                        onChange { it.copy(order = com.darkglass.launcher.model.Favorites.move(it.order, s.slot.id, -1)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    GlassButton("Descer", animate) {
                        onChange { it.copy(order = com.darkglass.launcher.model.Favorites.move(it.order, s.slot.id, +1)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    GlassButton("Associar", animate) { onAssociate(s.slot.id) }
                }
            }

            Section("Ao ligar a TV")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Abrir o Dark Glass", animate, selected = autostartEnabled) { onToggleAutostart(true) }
                GlassButton("Não abrir", animate, selected = !autostartEnabled) { onToggleAutostart(false) }
            }
            Spacer(Modifier.height(10.dp))
            if (canShowOnStartup) {
                DText("Permissão concedida: o Dark Glass abre sozinho quando a TV liga.", 13.sp, color = DG.Secondary)
            } else {
                DText(
                    "Falta a permissão \"Sobrepor a outros apps\". Sem ela o Android bloqueia a abertura automática. " +
                        "Conceda pelo botão abaixo ou, pelo Mac: adb shell appops set com.darkglass.launcher SYSTEM_ALERT_WINDOW allow",
                    13.sp,
                    color = DG.Secondary,
                )
                Spacer(Modifier.height(10.dp))
                GlassButton("Conceder permissão", animate, onClick = onGrantStartupPermission)
            }

            Section("Tela inicial")
            DText(
                "Ao ativar, o Dark Glass passa a ser oferecido como tela inicial. Depois pressione o botão " +
                    "Início do controle: se a TV perguntar, escolha Dark Glass. Se nada mudar, a TCL não " +
                    "permite trocar a tela inicial por meios oficiais — o Dark Glass continua funcionando " +
                    "como app comum.",
                13.sp,
                color = DG.Secondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Oferecer como tela inicial", animate, selected = homeCandidateEnabled) {
                    onToggleHomeCandidate(true)
                }
                GlassButton("Não oferecer", animate, selected = !homeCandidateEnabled) {
                    onToggleHomeCandidate(false)
                }
            }

            Section("Sistema e recuperação")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Configurações do Android", animate, onClick = onOpenAndroidSettings)
                GlassButton("Restaurar padrões", animate, onClick = onRestoreDefaults)
                GlassButton("Sair e voltar à tela original", animate, onClick = onExitToOriginal)
            }

            Section("Sobre")
            DText(
                "Dark Glass ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}) · sem anúncios, sem telemetria.",
                13.sp,
                color = DG.Secondary,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(22.dp))
    DText(title.uppercase(), 12.sp, color = DG.Ice, weight = FontWeight.SemiBold, letterSpacing = 2.sp)
    Spacer(Modifier.height(10.dp))
}

/** Lista de apps instalados para associar a um atalho (ex.: HD Player Pro). */
@Composable
fun AppPickerScreen(
    slotTitle: String,
    apps: List<InstalledApp>,
    current: String?,
    settings: UserSettings,
    onPick: (String?) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val animate = settings.animations
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight()
                .glassPanel(RoundedCornerShape(28.dp), glassIntensity(settings.glass))
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            DText("Associar: $slotTitle", 24.sp, weight = FontWeight.Light)
            Spacer(Modifier.height(6.dp))
            DText("Escolha o app instalado que este atalho deve abrir.", 13.sp, color = DG.Secondary)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Voltar", animate, Modifier.focusRequester(first), onClick = onBack)
                GlassButton("Automático (remover associação)", animate) { onPick(null) }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    GlassButton(
                        "${app.label}  ·  ${app.packageName}",
                        animate,
                        Modifier.fillMaxWidth(),
                        selected = app.packageName == current,
                    ) { onPick(app.packageName) }
                }
            }
        }
    }
}
