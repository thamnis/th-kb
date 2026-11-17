package com.thamnis.thkb.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class PersonalDictionaryState(
    val words: Map<String, Int> = emptyMap(),
    val bigrams: Map<String, Map<String, Int>> = emptyMap(),
    val blocked: Set<String> = emptySet(),
)

class PersonalDictionaryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<PersonalDictionaryState> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_PERSONAL_JSON) {
            _state.value = loadState()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun learnWord(prevWord: String, word: String, weightBoost: Int = 2) {
        val trimmed = word.trim()
        if (trimmed.length < 2) return
        if (!trimmed.all { it.isLetter() || it == '\'' || it == '-' }) return

        val current = _state.value
        val nextBlocked = current.blocked.toMutableSet()
        nextBlocked.remove(trimmed.lowercase(Locale.ROOT))

        val nextWords = current.words.toMutableMap()
        val existingKey = nextWords.keys.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (existingKey != null) {
            nextWords[existingKey] = (nextWords[existingKey] ?: 0) + weightBoost
        } else {
            // Limit stored learned vocabulary to top 2,500 entries to stay ultra-lightweight
            if (nextWords.size >= 2500) {
                val minEntry = nextWords.minByOrNull { it.value }
                if (minEntry != null) nextWords.remove(minEntry.key)
            }
            nextWords[trimmed] = weightBoost
        }

        val nextBigrams = current.bigrams.mapValues { it.value.toMutableMap() }.toMutableMap()
        val prevLower = prevWord.trim().lowercase(Locale.ROOT)
        if (prevLower.isNotEmpty() && prevLower.all { it.isLetter() || it == '\'' || it == '-' }) {
            val inner = nextBigrams.getOrPut(prevLower) { mutableMapOf() }
            inner[trimmed] = (inner[trimmed] ?: 0) + weightBoost
        }

        saveState(PersonalDictionaryState(nextWords, nextBigrams, nextBlocked))
    }

    fun addCustomWord(word: String) {
        learnWord("", word, weightBoost = 25)
    }

    fun removeWord(word: String) {
        val lower = word.trim().lowercase(Locale.ROOT)
        val current = _state.value
        val nextWords = current.words.filterKeys { it.lowercase(Locale.ROOT) != lower }
        saveState(current.copy(words = nextWords))
    }

    fun blockWord(word: String) {
        val lower = word.trim().lowercase(Locale.ROOT)
        if (lower.isEmpty()) return
        val current = _state.value
        val nextWords = current.words.filterKeys { it.lowercase(Locale.ROOT) != lower }
        val nextBlocked = current.blocked.toMutableSet().apply { add(lower) }
        saveState(current.copy(words = nextWords, blocked = nextBlocked))
    }

    fun unblockWord(word: String) {
        val lower = word.trim().lowercase(Locale.ROOT)
        val current = _state.value
        val nextBlocked = current.blocked.toMutableSet().apply { remove(lower) }
        saveState(current.copy(blocked = nextBlocked))
    }

    fun clearLearned() {
        saveState(PersonalDictionaryState())
    }

    fun toJson(target: PersonalDictionaryState = _state.value): String {
        val root = JSONObject()
        val wordsObj = JSONObject()
        target.words.forEach { (k, v) -> wordsObj.put(k, v) }
        root.put("words", wordsObj)

        val biObj = JSONObject()
        target.bigrams.forEach { (prev, inner) ->
            val innerObj = JSONObject()
            inner.forEach { (next, cnt) -> innerObj.put(next, cnt) }
            biObj.put(prev, innerObj)
        }
        root.put("bigrams", biObj)

        val blockedArr = JSONArray()
        target.blocked.forEach { blockedArr.put(it) }
        root.put("blocked", blockedArr)

        return root.toString()
    }

    private fun saveState(next: PersonalDictionaryState) {
        _state.value = next
        prefs.edit().putString(KEY_PERSONAL_JSON, toJson(next)).apply()
    }

    private fun loadState(): PersonalDictionaryState {
        val raw = prefs.getString(KEY_PERSONAL_JSON, null) ?: return PersonalDictionaryState()
        return parseState(raw)
    }

    fun destroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "th_kb_personal_dict"
        private const val KEY_PERSONAL_JSON = "personal_dict_json"

        fun parseState(raw: String): PersonalDictionaryState {
            return runCatching {
                val obj = JSONObject(raw)
                val words = mutableMapOf<String, Int>()
                obj.optJSONObject("words")?.let { wObj ->
                    val keys = wObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        words[k] = wObj.optInt(k, 1)
                    }
                }
                val bigrams = mutableMapOf<String, Map<String, Int>>()
                obj.optJSONObject("bigrams")?.let { bObj ->
                    val keys = bObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val innerObj = bObj.optJSONObject(k) ?: continue
                        val innerMap = mutableMapOf<String, Int>()
                        val iks = innerObj.keys()
                        while (iks.hasNext()) {
                            val ik = iks.next()
                            innerMap[ik] = innerObj.optInt(ik, 1)
                        }
                        bigrams[k] = innerMap
                    }
                }
                val blocked = mutableSetOf<String>()
                obj.optJSONArray("blocked")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val w = arr.optString(i, "").trim().lowercase(Locale.ROOT)
                        if (w.isNotEmpty()) blocked.add(w)
                    }
                }
                PersonalDictionaryState(words, bigrams, blocked)
            }.getOrDefault(PersonalDictionaryState())
        }
    }
}
