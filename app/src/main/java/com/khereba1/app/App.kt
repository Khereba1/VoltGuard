package com.khereba1.app

import android.app.Application
import android.content.Context

const val PREFS = "voltguard_prefs"

class App : Application()

object Prefs {
    private const val KEY_BYPASS = "bypass_enabled"
    private const val KEY_AUTOBOOT = "auto_boot"

    fun isBypassEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BYPASS, false)

    fun saveBypassEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BYPASS, enabled).apply()
    }

    fun isAutoBoot(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOBOOT, true)

    fun saveAutoBoot(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTOBOOT, enabled).apply()
    }
}
