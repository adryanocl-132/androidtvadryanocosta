package com.darkglass.launcher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.darkglass.launcher.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Arte real do app: banner 16:9 (padrão Android TV) ou, na falta, o ícone. */
data class AppArt(val bitmap: ImageBitmap, val isBanner: Boolean)

/**
 * Descoberta e abertura de apps via PackageManager.
 * Sem permissões especiais: a visibilidade vem do bloco <queries> do manifesto.
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, InstalledApp>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in query(intent)) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || pkg in result) continue
                result[pkg] = InstalledApp(pkg, info.loadLabel(pm).toString())
            }
        }
        result.values.sortedBy { it.label.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun query(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }

    suspend fun loadArt(pkg: String): AppArt? = withContext(Dispatchers.IO) {
        try {
            val banner: Drawable? = pm.getLeanbackLaunchIntentForPackage(pkg)?.component
                ?.let { runCatching { pm.getActivityBanner(it) }.getOrNull() }
                ?: runCatching { pm.getApplicationBanner(pkg) }.getOrNull()
            if (banner != null) {
                AppArt(banner.toBitmap(320, 180).asImageBitmap(), isBanner = true)
            } else {
                val icon = pm.getApplicationIcon(pkg)
                AppArt(icon.toBitmap(192, 192).asImageBitmap(), isBanner = false)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: RuntimeException) {
            null
        }
    }

    /** Abre o app preferindo a entrada de TV (Leanback). Retorna false se não conseguir. */
    fun launch(pkg: String): Boolean {
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
            ?: return false
        return start(intent)
    }

    fun openAndroidSettings(): Boolean = start(Intent(Settings.ACTION_SETTINGS))

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    // ---- Tela inicial (alias HOME), reversível ----

    private val homeAlias = ComponentName(context, "com.darkglass.launcher.HomeAlias")

    fun isHomeCandidateEnabled(): Boolean =
        pm.getComponentEnabledSetting(homeAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setHomeCandidateEnabled(enabled: Boolean) {
        pm.setComponentEnabledSetting(
            homeAlias,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    // ---- Abrir ao ligar a TV ----

    fun isAutostartEnabled(): Boolean = autostartEnabled(context)

    fun setAutostartEnabled(enabled: Boolean) {
        context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTOSTART, enabled).apply()
    }

    fun canShowOverStartup(): Boolean = Settings.canDrawOverlays(context)

    fun openOverlayPermissionScreen(): Boolean = start(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
    )

    companion object {
        const val BOOT_PREFS = "darkglass_boot"
        const val KEY_AUTOSTART = "autostart"
        fun autostartEnabled(context: Context): Boolean =
            context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTOSTART, true)
    }
}
