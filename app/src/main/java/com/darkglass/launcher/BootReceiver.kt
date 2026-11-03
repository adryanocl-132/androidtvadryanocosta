package com.darkglass.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.darkglass.launcher.data.AppRepository

/** Abre o Dark Glass quando a TV termina de ligar (se ativado nos Ajustes). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return
        if (!AppRepository.autostartEnabled(context)) return
        val launch = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(launch) }
    }
}
