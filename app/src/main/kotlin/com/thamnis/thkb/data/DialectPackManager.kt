package com.thamnis.thkb.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class DialectPack(
    val id: String,
    val name: String,
    val category: String,
    val language: String,
    val author: String,
    val version: String,
    val description: String,
    val wordCount: Int,
    val sampleWords: List<String>,
    val isBuiltIn: Boolean = true,
    val rawJson: String,
)

data class DialectStoreState(
    val availablePacks: List<DialectPack> = emptyList(),
    val enabledPackIds: Set<String> = DEFAULT_ENABLED_PACKS,
) {
    companion object {
        val DEFAULT_ENABLED_PACKS = setOf("nouchi_ci", "conversation")
    }
}

class DialectPackManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val builtInPacks: List<DialectPack> by lazy { loadBuiltInPacks() }

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<DialectStoreState> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _state.value = readState()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun togglePack(packId: String, enabled: Boolean) {
        val next = _state.value.enabledPackIds.toMutableSet()
        if (enabled) next.add(packId) else next.remove(packId)
        prefs.edit().putStringSet(KEY_ENABLED_IDS, next).apply()
        _state.value = readState()
    }

    fun importCustomPack(jsonText: String): Result<DialectPack> = runCatching {
        val pack = parsePack(jsonText, isBuiltIn = false)
            ?: error("Invalid dialect pack format. Expected 'name' and 'words' fields.")
        if (pack.wordCount == 0) {
            error("The dialect file contains no words.")
        }

        val currentCustom = loadCustomPacks().toMutableList()
        currentCustom.removeAll { it.id == pack.id }
        currentCustom.add(0, pack)

        val arr = JSONArray()
        currentCustom.forEach { arr.put(it.rawJson) }

        val nextEnabled = _state.value.enabledPackIds.toMutableSet().apply { add(pack.id) }
        prefs.edit()
            .putString(KEY_CUSTOM_PACKS, arr.toString())
            .putStringSet(KEY_ENABLED_IDS, nextEnabled)
            .apply()

        _state.value = readState()
        pack
    }

    fun removeCustomPack(packId: String) {
        val updated = loadCustomPacks().filterNot { it.id == packId }
        val arr = JSONArray()
        updated.forEach { arr.put(it.rawJson) }

        val nextEnabled = _state.value.enabledPackIds.toMutableSet().apply { remove(packId) }
        prefs.edit()
            .putString(KEY_CUSTOM_PACKS, arr.toString())
            .putStringSet(KEY_ENABLED_IDS, nextEnabled)
            .apply()

        _state.value = readState()
    }

    /**
     * Merges all enabled dialect packs into one consolidated DictionaryModel JSON string
     * for instant loading into Rust (`nativeSyncDialects`) and Kotlin fallback.
     */
    fun buildMergedOverlayJson(current: DialectStoreState = _state.value): String {
        val activePacks = current.availablePacks.filter { it.id in current.enabledPackIds }
        if (activePacks.isEmpty()) return ""

        val mergedWords = JSONObject()
        val mergedBigrams = JSONObject()
        val mergedTrigrams = JSONObject()

        activePacks.forEach { pack ->
            runCatching {
                val obj = JSONObject(pack.rawJson)
                obj.optJSONObject("words")?.let { wObj ->
                    val keys = wObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = wObj.optInt(k, 75000)
                        mergedWords.put(k, maxOf(mergedWords.optInt(k, 0), v))
                    }
                }
                obj.optJSONObject("bigrams")?.let { bObj ->
                    val keys = bObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val innerSrc = bObj.optJSONObject(k) ?: continue
                        val innerDst = mergedBigrams.optJSONObject(k) ?: JSONObject()
                        val iks = innerSrc.keys()
                        while (iks.hasNext()) {
                            val ik = iks.next()
                            innerDst.put(ik, maxOf(innerDst.optInt(ik, 0), innerSrc.optInt(ik, 900)))
                        }
                        mergedBigrams.put(k, innerDst)
                    }
                }
                obj.optJSONObject("trigrams")?.let { tObj ->
                    val keys = tObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val innerSrc = tObj.optJSONObject(k) ?: continue
                        val innerDst = mergedTrigrams.optJSONObject(k) ?: JSONObject()
                        val iks = innerSrc.keys()
                        while (iks.hasNext()) {
                            val ik = iks.next()
                            innerDst.put(ik, maxOf(innerDst.optInt(ik, 0), innerSrc.optInt(ik, 900)))
                        }
                        mergedTrigrams.put(k, innerDst)
                    }
                }
            }
        }

        return JSONObject().apply {
            put("words", mergedWords)
            put("bigrams", mergedBigrams)
            put("trigrams", mergedTrigrams)
        }.toString()
    }

    fun destroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readState(): DialectStoreState {
        val all = builtInPacks + loadCustomPacks()
        val enabled = prefs.getStringSet(KEY_ENABLED_IDS, null)?.toSet()
            ?: DialectStoreState.DEFAULT_ENABLED_PACKS
        return DialectStoreState(availablePacks = all, enabledPackIds = enabled)
    }

    private fun loadBuiltInPacks(): List<DialectPack> {
        val assetFiles = listOf(
            "dialects/nouchi_ci.json",
            "dialects/ci_local_loanwords.json",
            "dialects/african_urban_slang.json",
            "dialects/culinary_west_africa.json",
            "dialects/conversation.json",
            "dialects/abbr_fr.json",
            "dialects/abbr_en.json",
            "dialects/gaming_esports.json",
            "dialects/tech_cs.json",
            "dialects/medicine.json",
            "dialects/biology.json",
            "dialects/finance_momo.json",
            "dialects/law_legal.json",
            "dialects/engineering_arch.json",
        )
        return assetFiles.mapNotNull { path ->
            runCatching {
                val raw = appContext.assets.open(path).bufferedReader().use { it.readText() }
                parsePack(raw, isBuiltIn = true)
            }.getOrNull()
        }
    }

    private fun loadCustomPacks(): List<DialectPack> {
        val raw = prefs.getString(KEY_CUSTOM_PACKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val packJson = arr.optString(i, "")
                    if (packJson.isNotEmpty()) {
                        parsePack(packJson, isBuiltIn = false)?.let { add(it) }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "th_kb_dialect_store"
        private const val KEY_ENABLED_IDS = "enabled_pack_ids"
        private const val KEY_CUSTOM_PACKS = "custom_packs_json_array"

        fun createStarterTemplate(): String = """
            {
              "id": "custom_local_${System.currentTimeMillis() / 1000}",
              "name": "My Local Jargon",
              "category": "Community",
              "language": "all",
              "author": "Community Contributor",
              "version": "1.0",
              "description": "Custom vocabulary, local expressions, or specialized terminology.",
              "words": {
                "exampleword": 80000,
                "customterm": 75000
              },
              "bigrams": {
                "my": { "phrase": 950 }
              },
              "trigrams": {}
            }
        """.trimIndent()

        fun parsePack(rawJson: String, isBuiltIn: Boolean): DialectPack? {
            return runCatching {
                val obj = JSONObject(rawJson)
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) return null
                val rawId = obj.optString("id", "").trim()
                val id = if (rawId.isNotEmpty()) {
                    rawId
                } else {
                    name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')
                }
                val wordsObj = obj.optJSONObject("words") ?: return null
                val keys = mutableListOf<String>()
                val iter = wordsObj.keys()
                while (iter.hasNext()) {
                    keys.add(iter.next())
                }
                DialectPack(
                    id = id,
                    name = name,
                    category = obj.optString("category", if (isBuiltIn) "Built-in" else "Custom"),
                    language = obj.optString("language", "all"),
                    author = obj.optString("author", "Community"),
                    version = obj.optString("version", "1.0"),
                    description = obj.optString("description", "Custom dialect pack"),
                    wordCount = keys.size,
                    sampleWords = keys.take(5),
                    isBuiltIn = isBuiltIn,
                    rawJson = rawJson,
                )
            }.getOrNull()
        }
    }
}
