package com.darkglass.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.darkglass.launcher.data.AppArt
import com.darkglass.launcher.data.AppRepository
import com.darkglass.launcher.data.SettingsStore
import com.darkglass.launcher.data.UserSettings
import com.darkglass.launcher.model.Favorites
import com.darkglass.launcher.model.InstalledApp
import com.darkglass.launcher.model.SlotResolver
import com.darkglass.launcher.ui.AppPickerScreen
import com.darkglass.launcher.ui.HomeScreen
import com.darkglass.launcher.ui.SettingsScreen
import com.darkglass.launcher.ui.WallpaperBackground
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Picker(val slotId: String, val returnTo: Screen) : Screen
}

class MainActivity : ComponentActivity() {

    /** Muda a cada retorno ao app → reavalia apps instalados. */
    private val resumeTick = mutableIntStateOf(0)

    /** Muda quando o botão Início é pressionado com o app aberto → volta à tela principal. */
    private val homePressTick = mutableIntStateOf(0)

    private val launchedAsHome: Boolean
        get() = intent?.hasCategory(Intent.CATEGORY_HOME) == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(applicationContext)
        val repo = AppRepository(applicationContext)
        setContent {
            DarkGlassApp(
                store = store,
                repo = repo,
                resumeTick = resumeTick.intValue,
                homePressTick = homePressTick.intValue,
                isHome = launchedAsHome,
                toast = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() },
                exitToOriginal = {
                    repo.setHomeCandidateEnabled(false)
                    finishAffinity()
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homePressTick.intValue++
    }
}

@Composable
private fun DarkGlassApp(
    store: SettingsStore,
    repo: AppRepository,
    resumeTick: Int,
    homePressTick: Int,
    isHome: Boolean,
    toast: (String) -> Unit,
    exitToOriginal: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val loaded by store.settings.collectAsState(initial = null)
    var installed by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var homeCandidate by remember { mutableStateOf(repo.isHomeCandidateEnabled()) }
    var autostart by remember { mutableStateOf(repo.isAutostartEnabled()) }
    var canOverlay by remember { mutableStateOf(repo.canShowOverStartup()) }
    val arts = remember { mutableStateMapOf<String, AppArt>() }

    LaunchedEffect(resumeTick) {
        canOverlay = repo.canShowOverStartup()
        installed = repo.installedApps()
    }
    LaunchedEffect(homePressTick) { if (homePressTick > 0) screen = Screen.Home }

    val settings = loaded
    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(settings?.wallpaper ?: 0)
        if (settings != null) {
        val slots = remember(settings.order, settings.manual, installed) {
            SlotResolver.resolveAll(settings.order, installed, settings.manual)
        }
        LaunchedEffect(slots) {
            for (pkg in slots.mapNotNull { it.packageName }) {
                if (pkg !in arts) repo.loadArt(pkg)?.let { arts[pkg] = it }
            }
        }

        fun change(transform: (UserSettings) -> UserSettings) {
            scope.launch { store.update(transform) }
        }

        when (val s = screen) {
            Screen.Home -> {
                // Como tela inicial, Voltar não sai; como app comum, Voltar fecha normalmente.
                BackHandler(enabled = isHome) { }
                HomeScreen(
                    slots = slots,
                    arts = arts,
                    settings = settings,
                    focusedIndex = focusedIndex,
                    onFocusIndex = { focusedIndex = it },
                    onLaunch = { slot ->
                        val ok = slot.packageName?.let(repo::launch) ?: false
                        if (!ok) toast("Não foi possível abrir ${slot.slot.title}.")
                    },
                    onMissing = { slot -> screen = Screen.Picker(slot.slot.id, Screen.Home) },
                    onOpenSettings = { screen = Screen.Settings },
                )
            }

            Screen.Settings -> SettingsScreen(
                settings = settings,
                slots = slots,
                homeCandidateEnabled = homeCandidate,
                autostartEnabled = autostart,
                canShowOnStartup = canOverlay,
                onToggleAutostart = { enabled ->
                    repo.setAutostartEnabled(enabled)
                    autostart = repo.isAutostartEnabled()
                },
                onGrantStartupPermission = {
                    if (!repo.openOverlayPermissionScreen()) {
                        toast("Esta TV não tem essa tela. Conceda pelo Mac com o comando mostrado.")
                    }
                },
                onChange = ::change,
                onAssociate = { id -> screen = Screen.Picker(id, Screen.Settings) },
                onToggleHomeCandidate = { enabled ->
                    repo.setHomeCandidateEnabled(enabled)
                    homeCandidate = repo.isHomeCandidateEnabled()
                    toast(
                        if (enabled) "Pressione Início no controle para testar a escolha da tela inicial."
                        else "Dark Glass não será mais oferecido como tela inicial.",
                    )
                },
                onOpenAndroidSettings = {
                    if (!repo.openAndroidSettings()) toast("Não foi possível abrir as configurações.")
                },
                onRestoreDefaults = {
                    change { UserSettings() }
                    toast("Padrões restaurados.")
                },
                onExitToOriginal = exitToOriginal,
                onBack = { screen = Screen.Home },
            )

            is Screen.Picker -> {
                val slot = Favorites.byId(s.slotId)
                AppPickerScreen(
                    slotTitle = slot?.title ?: s.slotId,
                    apps = installed,
                    current = slots.firstOrNull { it.slot.id == s.slotId }?.packageName,
                    settings = settings,
                    onPick = { pkg ->
                        change { cur ->
                            val map = cur.manual.toMutableMap()
                            if (pkg == null) map.remove(s.slotId) else map[s.slotId] = pkg
                            cur.copy(manual = map)
                        }
                        screen = s.returnTo
                    },
                    onBack = { screen = s.returnTo },
                )
            }
        }
        }
    }
}
