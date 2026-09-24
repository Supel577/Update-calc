package com.example.calcvault.data.camouflage

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.R

enum class AppDisguise(
    val id: String,
    val label: String,
    val iconRes: Int,
    val description: String
) {
    CALCULATOR(
        id = "calculator",
        label = "Calculator",
        iconRes = R.drawable.custom_app_icon,
        description = "Standard secret calculator icon and title"
    ),
    WEATHER(
        id = "weather",
        label = "Weather",
        iconRes = R.drawable.ic_app_weather_real_1789885885665,
        description = "Camouflage as a daily Weather forecast app"
    ),
    NOTES(
        id = "notes",
        label = "Notes",
        iconRes = R.drawable.ic_app_notes_real_1789885876446,
        description = "Disguise as a simple notepad notebook app"
    ),
    MUSIC(
        id = "music",
        label = "Music Player",
        iconRes = R.drawable.ic_disguise_music_1789899183733,
        description = "Disguise as an audio/music player"
    );

    companion object {
        fun fromId(id: String?): AppDisguise {
            return entries.firstOrNull { it.id == id } ?: CALCULATOR
        }
    }
}

class AppCamouflageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("calc_vault_camouflage_prefs", Context.MODE_PRIVATE)

    private fun getComponentForDisguise(disguise: AppDisguise): ComponentName {
        val className = when (disguise) {
            AppDisguise.CALCULATOR -> "com.example.MainActivity"
            AppDisguise.WEATHER -> "com.example.MainActivityWeather"
            AppDisguise.NOTES -> "com.example.MainActivityNotes"
            AppDisguise.MUSIC -> "com.example.MainActivityMusic"
        }
        return ComponentName(context.packageName, className)
    }

    fun getCurrentDisguise(): AppDisguise {
        val savedId = prefs.getString(KEY_CURRENT_DISGUISE, AppDisguise.CALCULATOR.id)
        return AppDisguise.fromId(savedId)
    }

    fun applyDisguise(targetDisguise: AppDisguise): Boolean {
        return try {
            val pm = context.packageManager

            // Step 1: Enable the target disguise FIRST to avoid launcher having 0 components
            val targetComponent = getComponentForDisguise(targetDisguise)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Step 2: Disable all other disguises
            for (disguise in AppDisguise.entries) {
                if (disguise != targetDisguise) {
                    try {
                        val comp = getComponentForDisguise(disguise)
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    } catch (ce: Exception) {
                        android.util.Log.e("AppCamouflageManager", "Error disabling $disguise: ${ce.message}")
                    }
                }
            }

            prefs.edit().putString(KEY_CURRENT_DISGUISE, targetDisguise.id).apply()
            true
        } catch (e: Exception) {
            android.util.Log.e("AppCamouflageManager", "Failed to apply disguise: ${e.message}", e)
            false
        }
    }

    companion object {
        private const val KEY_CURRENT_DISGUISE = "key_current_disguise"
    }
}
