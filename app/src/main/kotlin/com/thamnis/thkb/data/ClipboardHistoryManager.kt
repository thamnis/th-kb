package com.thamnis.thkb.data

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ClipboardEntry(
    val text: String,
    val pinned: Boolean = false,
)

class ClipboardHistoryManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private val _entries = MutableStateFlow(loadEntries())
    val entries: StateFlow<List<ClipboardEntry>> = _entries.asStateFlow()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncFromSystemClipboard()
    }

    init {
        clipboard?.addPrimaryClipChangedListener(clipListener)
        syncFromSystemClipboard()
    }

    fun syncFromSystemClipboard() {
        val clip = clipboard?.primaryClip ?: return
        if (clip.itemCount <= 0) return
        val text = clip.getItemAt(0)?.coerceToText(appContext)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            addOrPromote(text)
        }
    }

    fun addOrPromote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = _entries.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.text == trimmed }
        val wasPinned = if (existingIdx >= 0) current.removeAt(existingIdx).pinned else false
        current.add(0, ClipboardEntry(text = trimmed, pinned = wasPinned))
        val trimmedList = current
            .sortedByDescending { it.pinned }
            .take(MAX_ITEMS)
        saveEntries(trimmedList)
    }

    fun togglePin(text: String) {
        val updated = _entries.value
            .map { if (it.text == text) it.copy(pinned = !it.pinned) else it }
            .sortedByDescending { it.pinned }
        saveEntries(updated)
    }

    fun remove(text: String) {
        val updated = _entries.value.filterNot { it.text == text }
        saveEntries(updated)
    }

    fun clearUnpinned() {
        val updated = _entries.value.filter { it.pinned }
        saveEntries(updated)
    }

    fun destroy() {
        clipboard?.removePrimaryClipChangedListener(clipListener)
    }

    private fun saveEntries(list: List<ClipboardEntry>) {
        _entries.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("text", item.text)
                    put("pinned", item.pinned)
                },
            )
        }
        prefs.edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
    }

    private fun loadEntries(): List<ClipboardEntry> {
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val text = obj.optString("text", "").trim()
                    if (text.isNotEmpty()) {
                        add(ClipboardEntry(text = text, pinned = obj.optBoolean("pinned", false)))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "th_kb_clipboard_prefs"
        private const val KEY_HISTORY_JSON = "history_json"
        private const val MAX_ITEMS = 25
    }
}
