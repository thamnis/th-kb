package com.thamnis.thkb.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ScratchNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

class NotesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _notes = MutableStateFlow(loadNotes())
    val notes: StateFlow<List<ScratchNote>> = _notes.asStateFlow()

    fun addNote(title: String, body: String): ScratchNote? {
        val cleanBody = body.trim()
        val cleanTitle = title.trim().ifEmpty {
            cleanBody.lineSequence().firstOrNull()?.take(32).orEmpty()
        }
        if (cleanTitle.isEmpty() && cleanBody.isEmpty()) return null
        val created = ScratchNote(title = cleanTitle, body = cleanBody)
        val next = (listOf(created) + _notes.value)
            .sortedWith(compareByDescending<ScratchNote> { it.pinned }.thenByDescending { it.updatedAt })
            .take(MAX_NOTES)
        saveNotes(next)
        return created
    }

    fun updateNote(id: String, title: String, body: String) {
        val cleanBody = body.trim()
        val cleanTitle = title.trim().ifEmpty {
            cleanBody.lineSequence().firstOrNull()?.take(32).orEmpty().ifEmpty { "Note" }
        }
        val next = _notes.value.map { note ->
            if (note.id == id) {
                note.copy(title = cleanTitle, body = cleanBody, updatedAt = System.currentTimeMillis())
            } else {
                note
            }
        }.sortedWith(compareByDescending<ScratchNote> { it.pinned }.thenByDescending { it.updatedAt })
        saveNotes(next)
    }

    fun togglePin(id: String) {
        val next = _notes.value.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }
            .sortedWith(compareByDescending<ScratchNote> { it.pinned }.thenByDescending { it.updatedAt })
        saveNotes(next)
    }

    fun deleteNote(id: String) {
        saveNotes(_notes.value.filterNot { it.id == id })
    }

    private fun saveNotes(list: List<ScratchNote>) {
        _notes.value = list
        val arr = JSONArray()
        list.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("body", n.body)
                    put("pinned", n.pinned)
                    put("updatedAt", n.updatedAt)
                },
            )
        }
        prefs.edit().putString(KEY_NOTES_JSON, arr.toString()).apply()
    }

    private fun loadNotes(): List<ScratchNote> {
        val raw = prefs.getString(KEY_NOTES_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "").trim()
                    val body = obj.optString("body", "").trim()
                    if (title.isNotEmpty() || body.isNotEmpty()) {
                        add(
                            ScratchNote(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                title = title,
                                body = body,
                                pinned = obj.optBoolean("pinned", false),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            ),
                        )
                    }
                }
            }.sortedWith(compareByDescending<ScratchNote> { it.pinned }.thenByDescending { it.updatedAt })
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "th_kb_notes_prefs"
        private const val KEY_NOTES_JSON = "notes_json"
        private const val MAX_NOTES = 40
    }
}
