package com.thamnis.thkb.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class TypingMetricsSnapshot(
    val totalWords: Int = 0,
    val totalChars: Int = 0,
    val complexWords: Int = 0,
    val swipeWords: Int = 0,
    val voiceWords: Int = 0,
    val uniqueWords: Set<String> = emptySet(),
) {
    val uniqueCount: Int
        get() = uniqueWords.size

    val averageWordLength: Double
        get() = if (totalWords <= 0) 0.0 else totalChars.toDouble() / totalWords.toDouble()

    val vocabularyRichnessPct: Double
        get() {
            if (totalWords <= 0) return 0.0
            val window = totalWords.coerceAtMost(MAX_UNIQUE_TRACKED).toDouble()
            return ((uniqueWords.size.toDouble() / window) * 100.0).coerceIn(0.0, 100.0)
        }

    val complexWordRatioPct: Double
        get() = if (totalWords <= 0) 0.0 else (complexWords.toDouble() / totalWords.toDouble()) * 100.0

    val cefrLevel: String
        get() = TypingMetricsManager.estimateCefrLevel(
            totalWords = totalWords,
            uniqueCount = uniqueWords.size,
            avgWordLen = averageWordLength,
            complexRatioPct = complexWordRatioPct,
        )

    companion object {
        const val MAX_UNIQUE_TRACKED = 1500
    }
}

class TypingMetricsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _snapshot = MutableStateFlow(loadSnapshot())
    val snapshot: StateFlow<TypingMetricsSnapshot> = _snapshot.asStateFlow()

    fun recordWord(rawWord: String, fromSwipe: Boolean = false, fromVoice: Boolean = false) {
        val clean = rawWord.trim { !it.isLetter() && it != '\'' && it != '-' }
        if (clean.length < 2 || !clean.any { it.isLetter() }) return
        val lower = clean.lowercase(Locale.ROOT)
        val letterLen = clean.count { it.isLetter() }
        val cur = _snapshot.value
        val nextUnique = cur.uniqueWords.toMutableSet().apply {
            if (size >= TypingMetricsSnapshot.MAX_UNIQUE_TRACKED && lower !in this) {
                // Keep a rolling window of tracked vocabulary richness
                val iter = iterator()
                if (iter.hasNext()) {
                    iter.next()
                    iter.remove()
                }
            }
            add(lower)
        }
        val next = cur.copy(
            totalWords = cur.totalWords + 1,
            totalChars = cur.totalChars + letterLen,
            complexWords = cur.complexWords + if (letterLen >= 7) 1 else 0,
            swipeWords = cur.swipeWords + if (fromSwipe) 1 else 0,
            voiceWords = cur.voiceWords + if (fromVoice) 1 else 0,
            uniqueWords = nextUnique,
        )
        saveSnapshot(next)
    }

    fun recordPhrase(text: String, fromVoice: Boolean = false) {
        text.split(Regex("\\s+")).forEach { token ->
            if (token.isNotBlank()) recordWord(token, fromSwipe = false, fromVoice = fromVoice)
        }
    }

    fun resetMetrics() {
        saveSnapshot(TypingMetricsSnapshot())
    }

    private fun saveSnapshot(state: TypingMetricsSnapshot) {
        _snapshot.value = state
        val obj = JSONObject().apply {
            put("totalWords", state.totalWords)
            put("totalChars", state.totalChars)
            put("complexWords", state.complexWords)
            put("swipeWords", state.swipeWords)
            put("voiceWords", state.voiceWords)
            put("uniqueWords", JSONArray(state.uniqueWords.toList()))
        }
        prefs.edit().putString(KEY_METRICS_JSON, obj.toString()).apply()
    }

    private fun loadSnapshot(): TypingMetricsSnapshot {
        val raw = prefs.getString(KEY_METRICS_JSON, null) ?: return TypingMetricsSnapshot()
        return runCatching {
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("uniqueWords")
            val uniq = linkedSetOf<String>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val w = arr.optString(i, "")
                    if (w.isNotEmpty()) uniq.add(w)
                }
            }
            TypingMetricsSnapshot(
                totalWords = obj.optInt("totalWords", 0),
                totalChars = obj.optInt("totalChars", 0),
                complexWords = obj.optInt("complexWords", 0),
                swipeWords = obj.optInt("swipeWords", 0),
                voiceWords = obj.optInt("voiceWords", 0),
                uniqueWords = uniq,
            )
        }.getOrDefault(TypingMetricsSnapshot())
    }

    companion object {
        private const val PREFS_NAME = "th_kb_metrics_prefs"
        private const val KEY_METRICS_JSON = "metrics_json"

        fun estimateCefrLevel(
            totalWords: Int,
            uniqueCount: Int,
            avgWordLen: Double,
            complexRatioPct: Double,
        ): String {
            if (totalWords < 15) return "Evaluating…"
            val diversity = uniqueCount.toDouble() / totalWords.coerceAtMost(1500).toDouble()
            val score = (diversity * 45.0) + (avgWordLen * 6.5) + (complexRatioPct * 0.9)
            return when {
                score >= 88.0 -> "C2 · Mastery"
                score >= 76.0 -> "C1 · Advanced"
                score >= 64.0 -> "B2 · Upper-Int"
                score >= 52.0 -> "B1 · Intermediate"
                score >= 40.0 -> "A2 · Elementary"
                else -> "A1 · Starter"
            }
        }
    }
}
