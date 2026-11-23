package com.thamnis.thkb.data

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class ClipboardEntry(
    val text: String,
    val pinned: Boolean = false,
    val mediaUri: String? = null,
    val mimeType: String? = null,
) {
    val isRichMedia: Boolean
        get() = !mediaUri.isNullOrEmpty()
}

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
        val item = clip.getItemAt(0) ?: return
        val uri = item.uri
        if (uri != null) {
            val mime = appContext.contentResolver.getType(uri)
                ?: (0 until clip.description.mimeTypeCount)
                    .map { clip.description.getMimeType(it) }
                    .firstOrNull { it.startsWith("image/") }
            if (mime != null && mime.startsWith("image/")) {
                val cached = cacheMediaFromUri(uri, mime)
                if (cached != null) {
                    val label = clip.description.label?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Image (${mime.substringAfter('/')})"
                    addOrPromote(text = label, mediaUri = cached.toString(), mimeType = mime)
                    return
                }
            }
        }
        val text = item.coerceToText(appContext)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            addOrPromote(text)
        }
    }

    private fun cacheMediaFromUri(uri: Uri, mimeType: String): Uri? = runCatching {
        val mediaDir = File(appContext.filesDir, "clipboard_media").apply { mkdirs() }
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { input ->
            val buf = input.readBytes()
            if (buf.size > 10 * 1024 * 1024) return@runCatching null
            buf
        } ?: return@runCatching null
        if (bytes.isEmpty()) return@runCatching null

        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .take(8)
            .joinToString("") { "%02x".format(it) }
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val outFile = File(mediaDir, "clip_$digest.$ext")
        if (!outFile.exists()) {
            outFile.writeBytes(bytes)
        }
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", outFile)
    }.getOrNull()

    fun addOrPromote(text: String, mediaUri: String? = null, mimeType: String? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = _entries.value.toMutableList()
        val existingIdx = if (mediaUri != null) {
            current.indexOfFirst { it.mediaUri == mediaUri }
        } else {
            current.indexOfFirst { it.text == trimmed && it.mediaUri == null }
        }
        val wasPinned = if (existingIdx >= 0) current.removeAt(existingIdx).pinned else false
        current.add(0, ClipboardEntry(text = trimmed, pinned = wasPinned, mediaUri = mediaUri, mimeType = mimeType))
        val trimmedList = current
            .sortedByDescending { it.pinned }
            .take(MAX_ITEMS)
        saveEntries(trimmedList)
    }

    fun togglePin(key: String) {
        val updated = _entries.value
            .map { if (it.mediaUri == key || it.text == key) it.copy(pinned = !it.pinned) else it }
            .sortedByDescending { it.pinned }
        saveEntries(updated)
    }

    fun remove(key: String) {
        val updated = _entries.value.filterNot { it.mediaUri == key || it.text == key }
        saveEntries(updated)
    }

    fun clearUnpinned() {
        val updated = _entries.value.filter { it.pinned }
        saveEntries(updated)
    }

    fun restorePinnedClips(clips: List<ClipboardEntry>) {
        if (clips.isEmpty()) return
        val current = _entries.value.toMutableList()
        clips.forEach { restored ->
            val idx = current.indexOfFirst { it.text == restored.text }
            if (idx >= 0) {
                current[idx] = current[idx].copy(pinned = true)
            } else {
                current.add(0, restored.copy(pinned = true))
            }
        }
        saveEntries(current.sortedByDescending { it.pinned }.take(MAX_ITEMS))
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
                    if (item.mediaUri != null) put("mediaUri", item.mediaUri)
                    if (item.mimeType != null) put("mimeType", item.mimeType)
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
                    val mediaUri = obj.optString("mediaUri", "").takeIf { it.isNotEmpty() }
                    val mimeType = obj.optString("mimeType", "").takeIf { it.isNotEmpty() }
                    if (text.isNotEmpty()) {
                        add(
                            ClipboardEntry(
                                text = text,
                                pinned = obj.optBoolean("pinned", false),
                                mediaUri = mediaUri,
                                mimeType = mimeType,
                            ),
                        )
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
