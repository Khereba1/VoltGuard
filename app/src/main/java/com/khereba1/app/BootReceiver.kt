package com.khereba1.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.isAutoBoot(context) || !Prefs.isBypassEnabled(context)) return

        val pending = goAsync()
        Thread {
            try {
                RootShell.setBypass(true)
            } catch (_: Exception) {
                // ignore
            } finally {
                pending.finish()
            }
        }.start()
    }
}
