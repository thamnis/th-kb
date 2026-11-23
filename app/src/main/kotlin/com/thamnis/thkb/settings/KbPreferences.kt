package com.thamnis.thkb.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.thamnis.thkb.data.ClipboardEntry
import com.thamnis.thkb.data.TodoTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class KbSettings(
    val layoutId: String = DEFAULT_LAYOUT_ID,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val autoCapOnOpen: Boolean = true,
    val showKeyPreview: Boolean = true,
    val longPressDelayMs: Long = DEFAULT_LONG_PRESS_DELAY_MS,
    val rapidDeleteDelayMs: Long = DEFAULT_RAPID_DELETE_DELAY_MS,
    val deleteGesturesEnabled: Boolean = true,
    val resetForwardDeleteOnChar: Boolean = false,
    val emojiGridColumns: Int = DEFAULT_EMOJI_GRID_COLUMNS,
    val favoriteItems: Set<String> = DEFAULT_FAVORITES,
    val recentEmojis: List<String> = emptyList(),
    val suggestionsEnabled: Boolean = true,
    val glideTypingEnabled: Boolean = true,
    val glideCharByChar: Boolean = false,
    val autocompleteMode: String = AUTOCOMPLETE_MODE_ADVANCED,
    val ivorianFrenchEnabled: Boolean = true,
    val dismissedGestureTip: Boolean = false,
    val keyboardErgonomicMode: String = ERGONOMIC_NORMAL,
    val floatingOffsetXFraction: Float = 0.5f,
    val floatingOffsetYDp: Float = 96f,
    val themeId: String = THEME_DARK_GRAPHITE,
    val remoteHostIp: String = "",
    val remoteHostPort: Int = DEFAULT_REMOTE_PORT,
) {
    companion object {
        const val DEFAULT_LAYOUT_ID = "qwerty_en"
        const val DEFAULT_LONG_PRESS_DELAY_MS = 450L
        const val DEFAULT_RAPID_DELETE_DELAY_MS = 580L
        const val DEFAULT_EMOJI_GRID_COLUMNS = 8
        const val AUTOCOMPLETE_MODE_STANDARD = "autocomplete"
        const val AUTOCOMPLETE_MODE_ADVANCED = "advanced_autocomplete"
        const val AUTOCOMPLETE_MODE_OFF = "off"
        const val ERGONOMIC_NORMAL = "normal"
        const val ERGONOMIC_SPLIT = "split"
        const val ERGONOMIC_ONE_HAND_LEFT = "one_hand_left"
        const val ERGONOMIC_ONE_HAND_RIGHT = "one_hand_right"
        const val ERGONOMIC_FLOATING = "floating"
        const val THEME_DARK_GRAPHITE = "dark_graphite"
        const val THEME_MATERIAL_YOU = "material_you"
        const val THEME_MIDNIGHT_AMOLED = "midnight_amoled"
        const val THEME_WARM_PAPER = "warm_paper"
        const val THEME_OCEAN_SLATE = "ocean_slate"
        const val THEME_FOREST_SAGE = "forest_sage"
        const val DEFAULT_REMOTE_PORT = 55677
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

    fun setShowKeyPreview(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_KEY_PREVIEW, enabled).apply()
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

    fun setSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUGGESTIONS_ENABLED, enabled).apply()
        _settings.value = readSettings()
    }

    fun setGlideTypingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLIDE_TYPING_ENABLED, enabled).apply()
        _settings.value = readSettings()
    }

    fun setGlideCharByChar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLIDE_CHAR_BY_CHAR, enabled).apply()
        _settings.value = readSettings()
    }

    fun setAutocompleteMode(mode: String) {
        prefs.edit().putString(KEY_AUTOCOMPLETE_MODE, mode).apply()
        _settings.value = readSettings()
    }

    fun setIvorianFrenchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IVORIAN_FRENCH_ENABLED, enabled).apply()
        _settings.value = readSettings()
    }

    fun setDismissedGestureTip(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_DISMISSED_GESTURE_TIP, dismissed).apply()
        _settings.value = readSettings()
    }

    fun setKeyboardErgonomicMode(mode: String) {
        prefs.edit().putString(KEY_ERGONOMIC_MODE, mode).apply()
        _settings.value = readSettings()
    }

    fun setFloatingPosition(xFraction: Float, yDp: Float) {
        prefs.edit()
            .putFloat(KEY_FLOATING_X_FRACTION, xFraction.coerceIn(0f, 1f))
            .putFloat(KEY_FLOATING_Y_DP, yDp.coerceIn(16f, 420f))
            .apply()
        _settings.value = readSettings()
    }

    fun setThemeId(themeId: String) {
        prefs.edit().putString(KEY_THEME_ID, themeId).apply()
        _settings.value = readSettings()
    }

    fun setRemoteHost(ip: String, port: Int = KbSettings.DEFAULT_REMOTE_PORT) {
        prefs.edit()
            .putString(KEY_REMOTE_HOST_IP, ip.trim())
            .putInt(KEY_REMOTE_HOST_PORT, port.coerceIn(1024, 65535))
            .apply()
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

    fun exportToJson(
        pinnedClips: List<ClipboardEntry> = emptyList(),
        todoTasks: List<TodoTask> = emptyList(),
    ): String = KbBackupCodec.encode(_settings.value, pinnedClips, todoTasks)

    private fun applySettingsToPrefs(restored: KbSettings) {
        prefs.edit()
            .putString(KEY_LAYOUT_ID, restored.layoutId)
            .putBoolean(KEY_HAPTICS, restored.hapticsEnabled)
            .putBoolean(KEY_SOUND, restored.soundEnabled)
            .putBoolean(KEY_AUTO_CAP_ON_OPEN, restored.autoCapOnOpen)
            .putBoolean(KEY_SHOW_KEY_PREVIEW, restored.showKeyPreview)
            .putLong(KEY_LONG_PRESS_DELAY_MS, restored.longPressDelayMs)
            .putLong(KEY_RAPID_DELETE_DELAY_MS, restored.rapidDeleteDelayMs)
            .putBoolean(KEY_DELETE_GESTURES, restored.deleteGesturesEnabled)
            .putBoolean(KEY_RESET_FORWARD_DELETE, restored.resetForwardDeleteOnChar)
            .putInt(KEY_EMOJI_GRID_COLUMNS, restored.emojiGridColumns)
            .putStringSet(KEY_FAVORITES, restored.favoriteItems)
            .putString(KEY_RECENT_EMOJIS, restored.recentEmojis.joinToString("\u001F"))
            .putBoolean(KEY_SUGGESTIONS_ENABLED, restored.suggestionsEnabled)
            .putBoolean(KEY_GLIDE_TYPING_ENABLED, restored.glideTypingEnabled)
            .putBoolean(KEY_GLIDE_CHAR_BY_CHAR, restored.glideCharByChar)
            .putString(KEY_AUTOCOMPLETE_MODE, restored.autocompleteMode)
            .putBoolean(KEY_IVORIAN_FRENCH_ENABLED, restored.ivorianFrenchEnabled)
            .putString(KEY_ERGONOMIC_MODE, restored.keyboardErgonomicMode)
            .putString(KEY_THEME_ID, restored.themeId)
            .apply()
        _settings.value = readSettings()
    }

    fun importFromJson(jsonText: String): Result<List<ClipboardEntry>> = runCatching {
        val (restored, pinnedClips, _) = KbBackupCodec.decodeFull(jsonText)
        applySettingsToPrefs(restored)
        pinnedClips
    }

    fun importFromJsonWithTodos(jsonText: String): Result<Pair<List<ClipboardEntry>, List<TodoTask>>> = runCatching {
        val (restored, pinnedClips, todoTasks) = KbBackupCodec.decodeFull(jsonText)
        applySettingsToPrefs(restored)
        pinnedClips to todoTasks
    }

    private fun readSettings(): KbSettings = KbSettings(
        layoutId = prefs.getString(KEY_LAYOUT_ID, KbSettings.DEFAULT_LAYOUT_ID)
            ?: KbSettings.DEFAULT_LAYOUT_ID,
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        soundEnabled = prefs.getBoolean(KEY_SOUND, false),
        autoCapOnOpen = prefs.getBoolean(KEY_AUTO_CAP_ON_OPEN, true),
        showKeyPreview = prefs.getBoolean(KEY_SHOW_KEY_PREVIEW, true),
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
        suggestionsEnabled = prefs.getBoolean(KEY_SUGGESTIONS_ENABLED, true),
        glideTypingEnabled = prefs.getBoolean(KEY_GLIDE_TYPING_ENABLED, true),
        glideCharByChar = prefs.getBoolean(KEY_GLIDE_CHAR_BY_CHAR, false),
        autocompleteMode = prefs.getString(KEY_AUTOCOMPLETE_MODE, KbSettings.AUTOCOMPLETE_MODE_ADVANCED)
            ?: KbSettings.AUTOCOMPLETE_MODE_ADVANCED,
        ivorianFrenchEnabled = prefs.getBoolean(KEY_IVORIAN_FRENCH_ENABLED, true),
        dismissedGestureTip = prefs.getBoolean(KEY_DISMISSED_GESTURE_TIP, false),
        keyboardErgonomicMode = prefs.getString(KEY_ERGONOMIC_MODE, KbSettings.ERGONOMIC_NORMAL)
            ?: KbSettings.ERGONOMIC_NORMAL,
        floatingOffsetXFraction = prefs.getFloat(KEY_FLOATING_X_FRACTION, 0.5f).coerceIn(0f, 1f),
        floatingOffsetYDp = prefs.getFloat(KEY_FLOATING_Y_DP, 96f).coerceIn(16f, 420f),
        themeId = prefs.getString(KEY_THEME_ID, KbSettings.THEME_DARK_GRAPHITE)
            ?: KbSettings.THEME_DARK_GRAPHITE,
        remoteHostIp = prefs.getString(KEY_REMOTE_HOST_IP, "") ?: "",
        remoteHostPort = prefs.getInt(KEY_REMOTE_HOST_PORT, KbSettings.DEFAULT_REMOTE_PORT),
    )

    companion object {
        private const val PREFS_NAME = "th_kb_prefs"
        private const val KEY_LAYOUT_ID = "layout_id"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_AUTO_CAP_ON_OPEN = "auto_cap_on_open"
        private const val KEY_SHOW_KEY_PREVIEW = "show_key_preview"
        private const val KEY_LONG_PRESS_DELAY_MS = "long_press_delay_ms"
        private const val KEY_RAPID_DELETE_DELAY_MS = "rapid_delete_delay_ms"
        private const val KEY_DELETE_GESTURES = "delete_gestures_enabled"
        private const val KEY_RESET_FORWARD_DELETE = "reset_forward_delete_on_char"
        private const val KEY_EMOJI_GRID_COLUMNS = "emoji_grid_columns"
        private const val KEY_FAVORITES = "favorite_items"
        private const val KEY_RECENT_EMOJIS = "recent_emojis"
        private const val KEY_SUGGESTIONS_ENABLED = "suggestions_enabled"
        private const val KEY_GLIDE_TYPING_ENABLED = "glide_typing_enabled"
        private const val KEY_GLIDE_CHAR_BY_CHAR = "glide_char_by_char"
        private const val KEY_AUTOCOMPLETE_MODE = "autocomplete_mode"
        private const val KEY_IVORIAN_FRENCH_ENABLED = "ivorian_french_enabled"
        private const val KEY_DISMISSED_GESTURE_TIP = "dismissed_gesture_tip"
        private const val KEY_ERGONOMIC_MODE = "keyboard_ergonomic_mode"
        private const val KEY_FLOATING_X_FRACTION = "floating_x_fraction"
        private const val KEY_FLOATING_Y_DP = "floating_y_dp"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_REMOTE_HOST_IP = "remote_host_ip"
        private const val KEY_REMOTE_HOST_PORT = "remote_host_port"
    }
}

object KbBackupCodec {
    data class DecodedBackup(
        val settings: KbSettings,
        val pinnedClips: List<ClipboardEntry>,
        val todoTasks: List<TodoTask>,
    )

    fun encode(
        settings: KbSettings,
        pinnedClips: List<ClipboardEntry> = emptyList(),
        todoTasks: List<TodoTask> = emptyList(),
    ): String {
        fun esc(s: String): String = buildString {
            append('"')
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
        fun arr(items: Iterable<String>): String = items.joinToString(", ", "[", "]") { esc(it) }

        return buildString {
            appendLine("{")
            appendLine("  \"version\": 1,")
            appendLine("  \"layoutId\": ${esc(settings.layoutId)},")
            appendLine("  \"hapticsEnabled\": ${settings.hapticsEnabled},")
            appendLine("  \"soundEnabled\": ${settings.soundEnabled},")
            appendLine("  \"autoCapOnOpen\": ${settings.autoCapOnOpen},")
            appendLine("  \"showKeyPreview\": ${settings.showKeyPreview},")
            appendLine("  \"longPressDelayMs\": ${settings.longPressDelayMs},")
            appendLine("  \"rapidDeleteDelayMs\": ${settings.rapidDeleteDelayMs},")
            appendLine("  \"deleteGesturesEnabled\": ${settings.deleteGesturesEnabled},")
            appendLine("  \"resetForwardDeleteOnChar\": ${settings.resetForwardDeleteOnChar},")
            appendLine("  \"emojiGridColumns\": ${settings.emojiGridColumns},")
            appendLine("  \"suggestionsEnabled\": ${settings.suggestionsEnabled},")
            appendLine("  \"glideTypingEnabled\": ${settings.glideTypingEnabled},")
            appendLine("  \"glideCharByChar\": ${settings.glideCharByChar},")
            appendLine("  \"autocompleteMode\": ${esc(settings.autocompleteMode)},")
            appendLine("  \"ivorianFrenchEnabled\": ${settings.ivorianFrenchEnabled},")
            appendLine("  \"keyboardErgonomicMode\": ${esc(settings.keyboardErgonomicMode)},")
            appendLine("  \"themeId\": ${esc(settings.themeId)},")
            appendLine("  \"favoriteItems\": ${arr(settings.favoriteItems)},")
            appendLine("  \"recentEmojis\": ${arr(settings.recentEmojis)},")
            appendLine("  \"pinnedClips\": ${arr(pinnedClips.filter { it.pinned }.map { it.text })},")
            append("  \"todoTasks\": [")
            val todoJsonList = todoTasks.map { task ->
                "{\"id\": ${esc(task.id)}, \"title\": ${esc(task.title)}, \"isCompleted\": ${task.isCompleted}, \"createdAt\": ${task.createdAt}}"
            }
            append(todoJsonList.joinToString(", "))
            appendLine("]")
            append("}")
        }
    }

    fun decode(jsonText: String): Pair<KbSettings, List<ClipboardEntry>> {
        val full = decodeFull(jsonText)
        return full.settings to full.pinnedClips
    }

    fun decodeFull(jsonText: String): DecodedBackup {
        fun unesc(raw: String): String = buildString {
            var i = 0
            while (i < raw.length) {
                if (raw[i] == '\\' && i + 1 < raw.length) {
                    when (val next = raw[i + 1]) {
                        '\\' -> append('\\')
                        '"' -> append('"')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        else -> append(next)
                    }
                    i += 2
                } else {
                    append(raw[i])
                    i++
                }
            }
        }
        fun stringVal(key: String, default: String): String {
            val m = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").find(jsonText)
            return if (m != null) unesc(m.groupValues[1]) else default
        }
        fun boolVal(key: String, default: Boolean): Boolean {
            val m = Regex("\"$key\"\\s*:\\s*(true|false)").find(jsonText)
            return m?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: default
        }
        fun longVal(key: String, default: Long): Long {
            val m = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(jsonText)
            return m?.groupValues?.get(1)?.toLongOrNull() ?: default
        }
        fun arrayVal(key: String): List<String>? {
            val m = Regex("\"$key\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL)).find(jsonText)
                ?: return null
            val body = m.groupValues[1]
            return Regex("\"((?:\\\\.|[^\"\\\\])*)\"").findAll(body)
                .map { unesc(it.groupValues[1]) }
                .toList()
        }

        val favorites = arrayVal("favoriteItems")?.toSet() ?: KbSettings.DEFAULT_FAVORITES
        val recents = arrayVal("recentEmojis") ?: emptyList()
        val pinnedClips = arrayVal("pinnedClips")
            ?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            ?.map { ClipboardEntry(text = it, pinned = true) }
            ?: emptyList()

        val todoTasks = runCatching {
            val m = Regex("\"todoTasks\"\\s*:\\s*\\[(.*?)]", setOf(RegexOption.DOT_MATCHES_ALL)).find(jsonText)
            if (m != null) {
                val body = m.groupValues[1]
                Regex("\\{(.*?)}").findAll(body).mapNotNull { itemMatch ->
                    val itemBody = itemMatch.groupValues[1]
                    val idMatch = Regex("\"id\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").find(itemBody)
                    val titleMatch = Regex("\"title\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").find(itemBody)
                    val completedMatch = Regex("\"isCompleted\"\\s*:\\s*(true|false)").find(itemBody)
                    val createdMatch = Regex("\"createdAt\"\\s*:\\s*(-?\\d+)").find(itemBody)
                    val title = titleMatch?.let { unesc(it.groupValues[1]) }.orEmpty()
                    if (title.isNotEmpty()) {
                        TodoTask(
                            id = idMatch?.let { unesc(it.groupValues[1]) } ?: java.util.UUID.randomUUID().toString(),
                            title = title,
                            isCompleted = completedMatch?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: false,
                            createdAt = createdMatch?.groupValues?.get(1)?.toLongOrNull() ?: System.currentTimeMillis(),
                        )
                    } else null
                }.toList()
            } else emptyList()
        }.getOrDefault(emptyList())

        val settings = KbSettings(
            layoutId = stringVal("layoutId", KbSettings.DEFAULT_LAYOUT_ID),
            hapticsEnabled = boolVal("hapticsEnabled", true),
            soundEnabled = boolVal("soundEnabled", false),
            autoCapOnOpen = boolVal("autoCapOnOpen", true),
            showKeyPreview = boolVal("showKeyPreview", true),
            longPressDelayMs = longVal("longPressDelayMs", KbSettings.DEFAULT_LONG_PRESS_DELAY_MS),
            rapidDeleteDelayMs = longVal("rapidDeleteDelayMs", KbSettings.DEFAULT_RAPID_DELETE_DELAY_MS),
            deleteGesturesEnabled = boolVal("deleteGesturesEnabled", true),
            resetForwardDeleteOnChar = boolVal("resetForwardDeleteOnChar", false),
            emojiGridColumns = longVal("emojiGridColumns", KbSettings.DEFAULT_EMOJI_GRID_COLUMNS.toLong())
                .toInt()
                .coerceIn(5, 10),
            favoriteItems = favorites,
            recentEmojis = recents,
            suggestionsEnabled = boolVal("suggestionsEnabled", true),
            glideTypingEnabled = boolVal("glideTypingEnabled", true),
            glideCharByChar = boolVal("glideCharByChar", false),
            autocompleteMode = stringVal("autocompleteMode", KbSettings.AUTOCOMPLETE_MODE_ADVANCED),
            ivorianFrenchEnabled = boolVal("ivorianFrenchEnabled", true),
            keyboardErgonomicMode = stringVal("keyboardErgonomicMode", KbSettings.ERGONOMIC_NORMAL),
            themeId = stringVal("themeId", KbSettings.THEME_DARK_GRAPHITE),
        )
        return DecodedBackup(settings, pinnedClips, todoTasks)
    }
}
