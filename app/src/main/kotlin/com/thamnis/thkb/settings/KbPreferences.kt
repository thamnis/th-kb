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
    val autoCapOnOpen: Boolean = true,
    val longPressDelayMs: Long = DEFAULT_LONG_PRESS_DELAY_MS,
    val rapidDeleteDelayMs: Long = DEFAULT_RAPID_DELETE_DELAY_MS,
    val deleteGesturesEnabled: Boolean = true,
    val resetForwardDeleteOnChar: Boolean = false,
    val emojiGridColumns: Int = DEFAULT_EMOJI_GRID_COLUMNS,
    val favoriteItems: Set<String> = DEFAULT_FAVORITES,
    val recentEmojis: List<String> = emptyList(),
) {
    companion object {
        const val DEFAULT_LAYOUT_ID = "qwerty_en"
        const val DEFAULT_LONG_PRESS_DELAY_MS = 450L
        const val DEFAULT_RAPID_DELETE_DELAY_MS = 580L
        const val DEFAULT_EMOJI_GRID_COLUMNS = 7
        val DEFAULT_FAVORITES = setOf("😂", "❤️", "👍", "🔥", "🙏", "(^_^)", "¯\\_(ツ)_/¯")
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

    fun setAutoCapOnOpen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CAP_ON_OPEN, enabled).apply()
        _settings.value = readSettings()
    }

    fun setLongPressDelayMs(delayMs: Long) {
        prefs.edit().putLong(KEY_LONG_PRESS_DELAY_MS, delayMs).apply()
        _settings.value = readSettings()
    }

    fun setRapidDeleteDelayMs(delayMs: Long) {
        prefs.edit().putLong(KEY_RAPID_DELETE_DELAY_MS, delayMs).apply()
        _settings.value = readSettings()
    }

    fun setDeleteGesturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DELETE_GESTURES, enabled).apply()
        _settings.value = readSettings()
    }

    fun setResetForwardDeleteOnChar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RESET_FORWARD_DELETE, enabled).apply()
        _settings.value = readSettings()
    }

    fun setEmojiGridColumns(columns: Int) {
        prefs.edit().putInt(KEY_EMOJI_GRID_COLUMNS, columns.coerceIn(5, 10)).apply()
        _settings.value = readSettings()
    }

    fun toggleFavoriteItem(item: String) {
        val current = _settings.value.favoriteItems.toMutableSet()
        if (!current.add(item)) {
            current.remove(item)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        _settings.value = readSettings()
    }

    fun recordRecentEmoji(emoji: String) {
        val current = _settings.value.recentEmojis.toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val trimmed = current.take(24)
        prefs.edit().putString(KEY_RECENT_EMOJIS, trimmed.joinToString("\u001F")).apply()
        _settings.value = readSettings()
    }

    private fun readSettings(): KbSettings = KbSettings(
        layoutId = prefs.getString(KEY_LAYOUT_ID, KbSettings.DEFAULT_LAYOUT_ID)
            ?: KbSettings.DEFAULT_LAYOUT_ID,
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        soundEnabled = prefs.getBoolean(KEY_SOUND, false),
        autoCapOnOpen = prefs.getBoolean(KEY_AUTO_CAP_ON_OPEN, true),
        longPressDelayMs = prefs.getLong(KEY_LONG_PRESS_DELAY_MS, KbSettings.DEFAULT_LONG_PRESS_DELAY_MS),
        rapidDeleteDelayMs = prefs.getLong(KEY_RAPID_DELETE_DELAY_MS, KbSettings.DEFAULT_RAPID_DELETE_DELAY_MS),
        deleteGesturesEnabled = prefs.getBoolean(KEY_DELETE_GESTURES, true),
        resetForwardDeleteOnChar = prefs.getBoolean(KEY_RESET_FORWARD_DELETE, false),
        emojiGridColumns = prefs.getInt(KEY_EMOJI_GRID_COLUMNS, KbSettings.DEFAULT_EMOJI_GRID_COLUMNS).coerceIn(5, 10),
        favoriteItems = prefs.getStringSet(KEY_FAVORITES, null)?.toSet()
            ?: KbSettings.DEFAULT_FAVORITES,
        recentEmojis = prefs.getString(KEY_RECENT_EMOJIS, "")
            ?.split("\u001F")
            ?.filter { it.isNotEmpty() }
            .orEmpty(),
    )

    companion object {
        private const val PREFS_NAME = "th_kb_prefs"
        private const val KEY_LAYOUT_ID = "layout_id"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_AUTO_CAP_ON_OPEN = "auto_cap_on_open"
        private const val KEY_LONG_PRESS_DELAY_MS = "long_press_delay_ms"
        private const val KEY_RAPID_DELETE_DELAY_MS = "rapid_delete_delay_ms"
        private const val KEY_DELETE_GESTURES = "delete_gestures_enabled"
        private const val KEY_RESET_FORWARD_DELETE = "reset_forward_delete_on_char"
        private const val KEY_EMOJI_GRID_COLUMNS = "emoji_grid_columns"
        private const val KEY_FAVORITES = "favorite_items"
        private const val KEY_RECENT_EMOJIS = "recent_emojis"
    }
}
