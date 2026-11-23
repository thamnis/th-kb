package com.thamnis.thkb.data

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _entries = MutableStateFlow(loadEntries())
    val entries: StateFlow<List<ClipboardEntry>> = _entries.asStateFlow()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncFromSystemClipboard(allowDelayedRetry = true)
    }

    private val screenshotObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            mainHandler.removeCallbacks(screenshotPollRunnable)
            mainHandler.postDelayed(screenshotPollRunnable, 500L)
        }
    }

    private val screenshotPollRunnable = Runnable {
        importRecentScreenshots(limit = 2)
    }

    init {
        clipboard?.addPrimaryClipChangedListener(clipListener)
        runCatching {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenshotObserver,
            )
        }
        syncFromSystemClipboard(allowDelayedRetry = false)
        importRecentScreenshots(limit = 3)
    }

    fun syncFromSystemClipboard(allowDelayedRetry: Boolean = false) {
        val clip = clipboard?.primaryClip ?: return
        if (clip.itemCount <= 0) return
        val item = clip.getItemAt(0) ?: return
        val uri = item.uri
        if (uri != null) {
            val rawLabel = clip.description.label?.toString().orEmpty()
            var mime = appContext.contentResolver.getType(uri)
                ?: (0 until clip.description.mimeTypeCount)
                    .map { clip.description.getMimeType(it) }
                    .firstOrNull { it.startsWith("image/") }
            if (mime == null) {
                mime = sniffImageMimeType(uri)
            }
            if (mime != null && mime.startsWith("image/")) {
                val cached = cacheMediaFromUri(uri, mime)
                if (cached != null) {
                    val timeTag = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val isScreenshot = rawLabel.contains("screenshot", ignoreCase = true) ||
                        uri.toString().contains("screenshot", ignoreCase = true)
                    val label = when {
                        isScreenshot -> "Screenshot · $timeTag"
                        rawLabel.isNotBlank() -> "$rawLabel · $timeTag"
                        else -> "Image (${mime.substringAfter('/')}) · $timeTag"
                    }
                    addOrPromote(text = label, mediaUri = cached.toString(), mimeType = mime)
                    return
                } else if (allowDelayedRetry) {
                    mainHandler.postDelayed({ syncFromSystemClipboard(allowDelayedRetry = false) }, 480L)
                    return
                }
            }
        }
        val text = item.coerceToText(appContext)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty() && !text.startsWith("content://")) {
            addOrPromote(text)
        }
    }

    fun importRecentScreenshots(limit: Int = 5) {
        if (!hasScreenshotPermission(appContext)) return
        Thread {
            runCatching {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATE_ADDED,
                )
                val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
                } else {
                    "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
                }
                val args = arrayOf("%Screenshot%", "%Screenshot%")
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val collected = mutableListOf<Triple<String, Uri, String>>()
                appContext.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    args,
                    sortOrder,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    while (cursor.moveToNext() && collected.size < limit) {
                        val id = cursor.getLong(idCol)
                        val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/png" else "image/png"
                        val dateSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                        val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                        val stamp = if (dateSec > 0L) {
                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(dateSec * 1000L))
                        } else {
                            name.ifBlank { "Screenshot" }
                        }
                        collected.add(Triple("Screenshot · $stamp", contentUri, mime))
                    }
                }
                // Insert oldest-first so the newest ends up at index 0
                for ((label, uri, mime) in collected.asReversed()) {
                    val cached = cacheMediaFromUri(uri, mime) ?: continue
                    val alreadyPresent = _entries.value.any { it.mediaUri == cached.toString() }
                    if (!alreadyPresent) {
                        addOrPromote(text = label, mediaUri = cached.toString(), mimeType = mime)
                    }
                }
            }
        }.start()
    }

    private fun sniffImageMimeType(uri: Uri): String? = runCatching {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, opts)
            opts.outMimeType
        }
    }.getOrNull()

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
        runCatching { appContext.contentResolver.unregisterContentObserver(screenshotObserver) }
        mainHandler.removeCallbacksAndMessages(null)
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

        fun hasScreenshotPermission(context: Context): Boolean {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }
}
