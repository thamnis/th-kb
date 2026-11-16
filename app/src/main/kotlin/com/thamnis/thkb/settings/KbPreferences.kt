package com.thamnis.thkb.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KbSettings(
    val layoutId: String = DEFAULT_LAYOUT_ID,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_LAYOUT_ID = "qwerty_en"
    }
}

class KbPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<KbSettings> = _settings.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = readSettings()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setLayoutId(layoutId: String) {
        prefs.edit().putString(KEY_LAYOUT_ID, layoutId).apply()
        _settings.value = readSettings()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _settings.value = readSettings()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settings.value = readSettings()
    }

    private fun readSettings(): KbSettings = KbSettings(
        layoutId = prefs.getString(KEY_LAYOUT_ID, KbSettings.DEFAULT_LAYOUT_ID)
            ?: KbSettings.DEFAULT_LAYOUT_ID,
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        soundEnabled = prefs.getBoolean(KEY_SOUND, false),
    )

    companion object {
        private const val PREFS_NAME = "th_kb_prefs"
        private const val KEY_LAYOUT_ID = "layout_id"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_SOUND = "sound_enabled"
    }
}
