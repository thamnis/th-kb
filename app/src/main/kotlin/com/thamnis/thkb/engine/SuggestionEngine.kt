package com.thamnis.thkb.engine

import android.content.Context
import com.thamnis.thkb.data.PersonalDictionaryState
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10

data class SuggestionCandidate(
    val word: String,
    val confidence: Double,
    val isExact: Boolean,
    val isLiteral: Boolean = false,
)

enum class CasingStyle {
    LOWER,
    TITLE,
    ALL_CAPS,
}

data class DictionaryModel(
    val words: Map<String, Int> = emptyMap(),
    val bigrams: Map<String, Map<String, Int>> = emptyMap(),
    val trigrams: Map<String, Map<String, Int>> = emptyMap(),
) {
    fun merge(other: DictionaryModel): DictionaryModel {
        val mergedWords = words.toMutableMap()
        for ((k, v) in other.words) {
            mergedWords[k] = maxOf(mergedWords[k] ?: 0, v)
        }

        val mergedBigrams = bigrams.mapValues { it.value.toMutableMap() }.toMutableMap()
        for ((k, nextMap) in other.bigrams) {
            val inner = mergedBigrams.getOrPut(k.lowercase(Locale.ROOT)) { mutableMapOf() }
            for ((nextW, score) in nextMap) {
                inner[nextW] = maxOf(inner[nextW] ?: 0, score)
            }
        }

        val mergedTrigrams = trigrams.mapValues { it.value.toMutableMap() }.toMutableMap()
        for ((k, nextMap) in other.trigrams) {
            val inner = mergedTrigrams.getOrPut(k.lowercase(Locale.ROOT)) { mutableMapOf() }
            for ((nextW, score) in nextMap) {
                inner[nextW] = maxOf(inner[nextW] ?: 0, score)
            }
        }

        return DictionaryModel(mergedWords, mergedBigrams, mergedTrigrams)
    }
}

private class WordEntry(
    val canonical: String,
    val lower: String,
    val clean: String,
    val logFreq: Double,
)

private class IndexedFallbackModel(
    val entries: List<WordEntry>,
    val exactClean: Map<String, List<Int>>,
    val prefix1: Map<Char, List<Int>>,
    val prefix2: Map<Pair<Char, Char>, List<Int>>,
    val symspellDeletes: Map<String, List<Int>>,
    val bigrams: Map<String, Map<String, Double>>,
    val trigrams: Map<String, Map<String, Double>>,
)

class SuggestionEngine(private val context: Context? = null) {
    private val cache = mutableMapOf<String, DictionaryModel>()
    private val indexedCache = mutableMapOf<String, IndexedFallbackModel>()

    @Volatile
    private var nativeInitAttempted = false

    @Volatile
    private var nativeReady = false

    @Volatile
    private var personalState = PersonalDictionaryState()

    @Volatile
    private var pendingPersonalJson: String? = null

    private fun readAssetOrEmpty(path: String): String {
        val ctx = context ?: return ""
        return runCatching {
            ctx.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    fun warmUpAsync() {
        if (context == null) return
        ensureNativeInitialized()
    }

    fun syncPersonalDictionary(state: PersonalDictionaryState, rawJson: String) {
        personalState = state
        pendingPersonalJson = rawJson
        if (nativeReady) {
            NativeSuggestionBridge.nativeSyncPersonalDictionary(rawJson)
        }
    }

    fun learnWord(prevWord: String, word: String, weightBoost: Int = 2) {
        val trimmed = word.trim()
        if (trimmed.length < 2) return
        val cur = personalState
        val nextWords = cur.words.toMutableMap()
        val key = nextWords.keys.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
        nextWords[key] = (nextWords[key] ?: 0) + weightBoost
        val nextBigrams = cur.bigrams.mapValues { it.value.toMutableMap() }.toMutableMap()
        val prevLower = prevWord.trim().lowercase(Locale.ROOT)
        if (prevLower.isNotEmpty()) {
            val inner = nextBigrams.getOrPut(prevLower) { mutableMapOf() }
            inner[trimmed] = (inner[trimmed] ?: 0) + weightBoost
        }
        val nextBlocked = cur.blocked.toMutableSet().apply { remove(trimmed.lowercase(Locale.ROOT)) }
        personalState = PersonalDictionaryState(nextWords, nextBigrams, nextBlocked)

        if (nativeReady) {
            NativeSuggestionBridge.nativeLearnWord(prevWord, trimmed, weightBoost)
        }
    }

    fun blockWord(word: String) {
        val lower = word.trim().lowercase(Locale.ROOT)
        if (lower.isEmpty()) return
        val cur = personalState
        val nextWords = cur.words.filterKeys { it.lowercase(Locale.ROOT) != lower }
        val nextBlocked = cur.blocked.toMutableSet().apply { add(lower) }
        personalState = cur.copy(words = nextWords, blocked = nextBlocked)

        if (nativeReady) {
            NativeSuggestionBridge.nativeBlockWord(word)
        }
    }

    @Synchronized
    private fun ensureNativeInitialized(): Boolean {
        if (nativeReady) return true
        if (nativeInitAttempted || !NativeSuggestionBridge.isLoaded || context == null) {
            return nativeReady
        }
        nativeInitAttempted = true
        val enTsv = readAssetOrEmpty("dictionaries/en_words.txt")
        val enJson = readAssetOrEmpty("dictionaries/en_common.json")
        val frTsv = readAssetOrEmpty("dictionaries/fr_words.txt")
        val frJson = readAssetOrEmpty("dictionaries/fr_common.json")
        val frCiJson = readAssetOrEmpty("dictionaries/fr_ci_nouchi.json")
        nativeReady = NativeSuggestionBridge.nativeInitDictionaries(
            enTsv = enTsv,
            enCommonJson = enJson,
            frTsv = frTsv,
            frCommonJson = frJson,
            frCiJson = frCiJson,
        )
        if (nativeReady) {
            pendingPersonalJson?.let { json ->
                NativeSuggestionBridge.nativeSyncPersonalDictionary(json)
            }
        }
        return nativeReady
    }

    fun loadDictionary(name: String): DictionaryModel {
        return cache.getOrPut(name) {
            val jsonText = readAssetOrEmpty("dictionaries/$name.json")
            if (jsonText.isEmpty()) DictionaryModel() else parseDictionary(jsonText)
        }
    }

    fun getActiveDictionary(layoutId: String, ivorianEnabled: Boolean): DictionaryModel {
        val isFr = layoutId.contains("fr") || layoutId.contains("ci")
        val baseKey = if (isFr) "fr_common" else "en_common"
        val base = loadDictionary(baseKey)
        return if (ivorianEnabled && isFr) {
            base.merge(loadDictionary("fr_ci_nouchi"))
        } else {
            base
        }
    }

    private fun getFallbackIndex(layoutId: String, ivorianEnabled: Boolean): IndexedFallbackModel {
        val isFr = layoutId.contains("fr") || layoutId.contains("ci")
        val cacheKey = "${if (isFr) "fr" else "en"}_$ivorianEnabled"
        return indexedCache.getOrPut(cacheKey) {
            val tsv = readAssetOrEmpty(if (isFr) "dictionaries/fr_words.txt" else "dictionaries/en_words.txt")
            val jsonModel = getActiveDictionary(layoutId, ivorianEnabled)
            buildIndexedFallback(tsv, jsonModel)
        }
    }

    /**
     * Extracts sweet-spot context from text before cursor.
     * Takes up to 96 characters, extracts up to 2 preceding words and active prefix.
     */
    fun extractContext(textBeforeCursor: CharSequence?): Pair<List<String>, String> {
        if (textBeforeCursor.isNullOrEmpty()) return emptyList<String>() to ""
        val window = textBeforeCursor.takeLast(96).toString()

        val endsWithSpace = window.endsWith(" ") || window.endsWith("\t") || window.endsWith("\n")
        val tokens = window.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        return if (endsWithSpace) {
            val contextWords = tokens.takeLast(2)
            contextWords to ""
        } else {
            val prefix = tokens.lastOrNull() ?: ""
            val contextWords = tokens.dropLast(1).takeLast(2)
            contextWords to prefix
        }
    }

    /**
     * Queries suggestions using native Rust engine when available, falling back cleanly
     * to the matching Kotlin engine during JVM unit tests or non-NDK builds.
     */
    fun suggestForInput(
        layoutId: String,
        contextWords: List<String>,
        prefix: String,
        mode: String,
        ivorianEnabled: Boolean,
        capitalizeHint: Boolean = false,
        maxResults: Int = 3,
    ): List<SuggestionCandidate> {
        if (ensureNativeInitialized()) {
            val ctxJoined = contextWords.joinToString("\u001F")
            val json = NativeSuggestionBridge.nativeQuerySuggestions(
                layoutId = layoutId,
                contextJoined = ctxJoined,
                prefix = prefix,
                mode = mode,
                ivorianEnabled = ivorianEnabled,
                capitalizeHint = capitalizeHint,
                maxResults = maxResults,
            )
            if (json.isNotEmpty()) {
                return parseNativeCandidates(json)
            }
        }
        val fallbackIdx = getFallbackIndex(layoutId, ivorianEnabled)
        return queryIndexed(
            index = fallbackIdx,
            contextWords = contextWords,
            prefix = prefix,
            mode = mode,
            layoutId = layoutId,
            capitalizeHint = capitalizeHint,
            maxResults = maxResults,
        )
    }

    /**
     * Determines if committing a separator (Space / punctuation) should auto-replace the typed prefix.
     */
    fun findAutoReplacement(
        layoutId: String,
        contextWords: List<String>,
        prefix: String,
        mode: String,
        ivorianEnabled: Boolean,
        capitalizeHint: Boolean = false,
    ): SuggestionCandidate? {
        val trimmed = prefix.trim()
        if (trimmed.length < 2) return null
        val candidates = suggestForInput(
            layoutId = layoutId,
            contextWords = contextWords,
            prefix = trimmed,
            mode = mode,
            ivorianEnabled = ivorianEnabled,
            capitalizeHint = capitalizeHint,
            maxResults = 5,
        )
        return selectAutoReplacement(trimmed, candidates, mode, layoutId)
    }

    /**
     * Direct in-memory dictionary query (primarily used for unit testing and custom overlays).
     */
    fun querySuggestions(
        dict: DictionaryModel,
        contextWords: List<String>,
        prefix: String,
        mode: String,
        maxResults: Int = 3,
        layoutId: String = "azerty_fr",
        capitalizeHint: Boolean = false,
    ): List<SuggestionCandidate> {
        val index = buildIndexedFallback("", dict)
        return queryIndexed(
            index = index,
            contextWords = contextWords,
            prefix = prefix,
            mode = mode,
            layoutId = layoutId,
            capitalizeHint = capitalizeHint,
            maxResults = maxResults,
        )
    }

    private fun queryIndexed(
        index: IndexedFallbackModel,
        contextWords: List<String>,
        prefix: String,
        mode: String,
        layoutId: String,
        capitalizeHint: Boolean,
        maxResults: Int,
    ): List<SuggestionCandidate> {
        val isAdvanced = mode == "advanced_autocomplete"
        val trimmedPrefix = prefix.trim()
        val normLower = trimmedPrefix.lowercase(Locale.ROOT)
        val cleanPrefix = stripAccents(normLower)
        val casing = detectCasing(trimmedPrefix, capitalizeHint)

        // Clean Start: do not display raw filler unigrams on an empty field.
        if (cleanPrefix.isEmpty()) {
            if (contextWords.isEmpty()) return emptyList()
            return queryNextWord(index, contextWords, isAdvanced, casing, maxResults)
        }

        val personal = personalState
        fun isBlocked(w: String): Boolean = personal.blocked.contains(w.lowercase(Locale.ROOT))
        fun personalWordBoost(w: String): Double {
            val cnt = personal.words.entries
                .firstOrNull { it.key.equals(w, ignoreCase = true) }
                ?.value ?: return 0.0
            return log10(cnt.toDouble() + 1.0) * 95.0
        }

        val scored = mutableListOf<Triple<String, Double, Boolean>>()
        val seenIdx = HashSet<Int>()

        // 0. Personal & custom learned words
        for ((pWord, cnt) in personal.words) {
            if (isBlocked(pWord)) continue
            val pLower = pWord.lowercase(Locale.ROOT)
            val pClean = stripAccents(pLower)
            val boost = log10(cnt.toDouble() + 1.0) * 140.0
            if (pClean == cleanPrefix) {
                var score = 500.0 + boost
                score += contextBoost(index, pLower, contextWords, isAdvanced)
                scored.add(Triple(pWord, score, true))
            } else if (pClean.startsWith(cleanPrefix)) {
                val lenDiff = (pClean.length - cleanPrefix.length).coerceAtLeast(0)
                var score = 460.0 + boost - lenDiff * 12.0
                score += contextBoost(index, pLower, contextWords, isAdvanced)
                scored.add(Triple(pWord, score, false))
            }
        }

        // 1. Exact clean match
        index.exactClean[cleanPrefix]?.forEach { idx ->
            if (seenIdx.add(idx)) {
                val entry = index.entries[idx]
                if (!isBlocked(entry.canonical)) {
                    val isExactDiacritic = entry.lower == normLower
                    var score = entry.logFreq + if (isExactDiacritic) 320.0 else 260.0
                    score += contextBoost(index, entry.lower, contextWords, isAdvanced)
                    score += personalWordBoost(entry.canonical)
                    scored.add(Triple(entry.canonical, score, true))
                }
            }
        }

        // 2. Prefix bucket lookup (sorted descending by log frequency)
        val bucket = when {
            cleanPrefix.length >= 2 -> index.prefix2[cleanPrefix[0] to cleanPrefix[1]]
            cleanPrefix.length == 1 -> index.prefix1[cleanPrefix[0]]
            else -> null
        }

        if (bucket != null) {
            var hits = 0
            for (idx in bucket) {
                if (seenIdx.contains(idx)) continue
                val entry = index.entries[idx]
                if (entry.clean.startsWith(cleanPrefix)) {
                    seenIdx.add(idx)
                    if (isBlocked(entry.canonical)) continue
                    val lenDiff = (entry.clean.length - cleanPrefix.length).coerceAtLeast(0)
                    val exactPrefix = entry.lower.startsWith(normLower)
                    var score = entry.logFreq + (if (exactPrefix) 180.0 else 140.0) - lenDiff * 14.0
                    score += contextBoost(index, entry.lower, contextWords, isAdvanced)
                    score += personalWordBoost(entry.canonical)
                    scored.add(Triple(entry.canonical, score, false))
                    hits++
                    if (hits >= 60) break
                }
            }
        }

        // 3. SymSpell distance-1 typo + spatial proximity (Advanced mode)
        if (isAdvanced && cleanPrefix.length >= 3) {
            val probeKeys = buildList(cleanPrefix.length + 1) {
                add(cleanPrefix)
                for (skip in cleanPrefix.indices) {
                    add(cleanPrefix.removeRange(skip, skip + 1))
                }
            }
            var checked = 0
            for (key in probeKeys) {
                val hitList = index.symspellDeletes[key] ?: continue
                for (idx in hitList) {
                    if (seenIdx.contains(idx)) continue
                    val entry = index.entries[idx]
                    if (isBlocked(entry.canonical)) continue
                    if (abs(entry.clean.length - cleanPrefix.length) > 1) continue
                    val dist = spatialDamerauDistance(cleanPrefix, entry.clean, layoutId)
                    if (dist <= 1.15) {
                        seenIdx.add(idx)
                        var score = entry.logFreq + (1.2 - dist) * 190.0
                        score += contextBoost(index, entry.lower, contextWords, true)
                        score += personalWordBoost(entry.canonical)
                        scored.add(Triple(entry.canonical, score, false))
                        checked++
                        if (checked >= 40) break
                    }
                }
                if (checked >= 40) break
            }
        }

        scored.sortByDescending { it.second }

        val results = mutableListOf<SuggestionCandidate>()
        for ((word, score, isExact) in scored) {
            val cased = applyCasing(word, casing)
            if (results.none { it.word.equals(cased, ignoreCase = true) }) {
                results.add(SuggestionCandidate(cased, score, isExact))
                if (results.size >= maxResults) break
            }
        }
        return results
    }

    private fun queryNextWord(
        index: IndexedFallbackModel,
        contextWords: List<String>,
        isAdvanced: Boolean,
        casing: CasingStyle,
        maxResults: Int,
    ): List<SuggestionCandidate> {
        val personal = personalState
        fun isBlocked(w: String): Boolean = personal.blocked.contains(w.lowercase(Locale.ROOT))
        val nextCandidates = mutableListOf<Pair<String, Double>>()

        if (isAdvanced && contextWords.size >= 2) {
            val lastTwo = "${contextWords[contextWords.size - 2].lowercase(Locale.ROOT)} ${contextWords.last().lowercase(Locale.ROOT)}"
            index.trigrams[lastTwo]?.forEach { (w, s) ->
                if (!isBlocked(w)) nextCandidates.add(w to s * 2.2)
            }
        }

        contextWords.lastOrNull()?.lowercase(Locale.ROOT)?.let { prev ->
            personal.bigrams[prev]?.forEach { (w, cnt) ->
                if (!isBlocked(w)) {
                    val s = log10(cnt.toDouble() + 1.0) * 180.0 + 480.0
                    nextCandidates.add(w to s)
                }
            }
            index.bigrams[prev]?.forEach { (w, s) ->
                if (!isBlocked(w) && nextCandidates.none { it.first.equals(w, ignoreCase = true) }) {
                    nextCandidates.add(w to s * 1.4)
                }
            }
        }

        if (nextCandidates.isEmpty()) return emptyList()

        nextCandidates.sortByDescending { it.second }

        val results = mutableListOf<SuggestionCandidate>()
        for ((word, score) in nextCandidates) {
            val cased = applyCasing(word, casing)
            if (results.none { it.word.equals(cased, ignoreCase = true) }) {
                results.add(SuggestionCandidate(cased, score, false))
                if (results.size >= maxResults) break
            }
        }
        return results
    }

    private fun contextBoost(
        index: IndexedFallbackModel,
        wordLower: String,
        contextWords: List<String>,
        isAdvanced: Boolean,
    ): Double {
        if (!isAdvanced || contextWords.isEmpty()) return 0.0
        var boost = 0.0
        if (contextWords.size >= 2) {
            val lastTwo = "${contextWords[contextWords.size - 2].lowercase(Locale.ROOT)} ${contextWords.last().lowercase(Locale.ROOT)}"
            index.trigrams[lastTwo]?.get(wordLower)?.let { boost += it * 1.5 }
        }
        contextWords.lastOrNull()?.lowercase(Locale.ROOT)?.let { prev ->
            personalState.bigrams[prev]?.forEach { (pw, cnt) ->
                if (pw.lowercase(Locale.ROOT) == wordLower) {
                    boost += log10(cnt.toDouble() + 1.0) * 140.0 + 120.0
                }
            }
            index.bigrams[prev]?.get(wordLower)?.let { boost += it * 0.9 }
        }
        return boost
    }

    private fun parseNativeCandidates(jsonText: String): List<SuggestionCandidate> {
        return runCatching {
            val arr = JSONArray(jsonText)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        SuggestionCandidate(
                            word = obj.getString("word"),
                            confidence = obj.optDouble("confidence", 0.0),
                            isExact = obj.optBoolean("is_exact", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun buildIndexedFallback(wordTsv: String, jsonModel: DictionaryModel): IndexedFallbackModel {
        val wordCounts = HashMap<String, Long>()
        if (wordTsv.isNotEmpty()) {
            wordTsv.lineSequence().forEach { line ->
                val tabIdx = line.indexOf('\t')
                val sepIdx = if (tabIdx >= 0) tabIdx else line.indexOf(' ')
                if (sepIdx > 0 && sepIdx + 1 < line.length) {
                    val w = line.substring(0, sepIdx)
                    val c = line.substring(sepIdx + 1).trim().toLongOrNull()
                    if (c != null) {
                        wordCounts[w] = maxOf(wordCounts[w] ?: 0L, c)
                    }
                }
            }
        }
        for ((w, c) in jsonModel.words) {
            wordCounts[w] = maxOf(wordCounts[w] ?: 0L, c.toLong())
        }

        val entries = wordCounts.entries
            .map { (canonical, freq) ->
                val lower = canonical.lowercase(Locale.ROOT)
                val clean = stripAccents(lower)
                WordEntry(canonical, lower, clean, log10(freq.toDouble() + 1.0) * 100.0)
            }
            .sortedByDescending { it.logFreq }

        val exactClean = HashMap<String, MutableList<Int>>()
        val prefix1 = HashMap<Char, MutableList<Int>>()
        val prefix2 = HashMap<Pair<Char, Char>, MutableList<Int>>()
        val symspell = HashMap<String, MutableList<Int>>()

        entries.forEachIndexed { idx, entry ->
            exactClean.getOrPut(entry.clean) { mutableListOf() }.add(idx)
            if (entry.clean.isNotEmpty()) {
                val c1 = entry.clean[0]
                prefix1.getOrPut(c1) { mutableListOf() }.add(idx)
                if (entry.clean.length >= 2) {
                    prefix2.getOrPut(c1 to entry.clean[1]) { mutableListOf() }.add(idx)
                }
            }
            if (idx < 15_000 && entry.clean.length >= 3) {
                for (skip in entry.clean.indices) {
                    val del = entry.clean.removeRange(skip, skip + 1)
                    val list = symspell.getOrPut(del) { mutableListOf() }
                    if (list.size < 24 && !list.contains(idx)) {
                        list.add(idx)
                    }
                }
            }
        }

        val logBigrams = jsonModel.bigrams.mapValues { (_, inner) ->
            inner.mapValues { (_, c) -> log10(c.toDouble() + 1.0) * 100.0 }
        }
        val logTrigrams = jsonModel.trigrams.mapValues { (_, inner) ->
            inner.mapValues { (_, c) -> log10(c.toDouble() + 1.0) * 100.0 }
        }

        return IndexedFallbackModel(
            entries = entries,
            exactClean = exactClean,
            prefix1 = prefix1,
            prefix2 = prefix2,
            symspellDeletes = symspell,
            bigrams = logBigrams,
            trigrams = logTrigrams,
        )
    }

    companion object {
        fun detectCasing(prefix: String, capitalizeHint: Boolean): CasingStyle {
            val letters = prefix.filter { it.isLetter() }
            return when {
                letters.length >= 2 && letters.all { it.isUpperCase() } -> CasingStyle.ALL_CAPS
                letters.firstOrNull()?.isUpperCase() == true || capitalizeHint -> CasingStyle.TITLE
                else -> CasingStyle.LOWER
            }
        }

        fun applyCasing(canonical: String, style: CasingStyle): String = when (style) {
            CasingStyle.ALL_CAPS -> canonical.uppercase(Locale.ROOT)
            CasingStyle.TITLE -> canonical.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
            CasingStyle.LOWER -> canonical
        }

        fun stripAccents(s: String): String {
            val out = StringBuilder(s.length)
            for (c in s) {
                when (c) {
                    'à', 'á', 'â', 'ã', 'ä', 'å' -> out.append('a')
                    'è', 'é', 'ê', 'ë' -> out.append('e')
                    'ì', 'í', 'î', 'ï' -> out.append('i')
                    'ò', 'ó', 'ô', 'õ', 'ö' -> out.append('o')
                    'ù', 'ú', 'û', 'ü' -> out.append('u')
                    'ç' -> out.append('c')
                    'ñ' -> out.append('n')
                    'ÿ' -> out.append('y')
                    else -> out.append(c)
                }
            }
            return out.toString()
        }

        private fun keyCoords(ch: Char, layoutId: String): Pair<Double, Double>? {
            val rows = if (layoutId.contains("fr")) {
                listOf("azertyuiop", "qsdfghjklm", "wxcvbn")
            } else {
                listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
            }
            val offsets = listOf(0.0, 0.25, 0.75)
            rows.forEachIndexed { rIdx, row ->
                val cIdx = row.indexOf(ch)
                if (cIdx >= 0) return rIdx.toDouble() to (cIdx + offsets[rIdx])
            }
            return null
        }

        fun spatialDamerauDistance(a: String, b: String, layoutId: String): Double {
            val m = a.length
            val n = b.length
            val dp = Array(m + 1) { DoubleArray(n + 1) }
            for (i in 0..m) dp[i][0] = i.toDouble()
            for (j in 0..n) dp[0][j] = j.toDouble()

            for (i in 1..m) {
                for (j in 1..n) {
                    val ca = a[i - 1]
                    val cb = b[j - 1]
                    val subCost = if (ca == cb) {
                        0.0
                    } else {
                        val pa = keyCoords(ca, layoutId)
                        val pb = keyCoords(cb, layoutId)
                        if (pa != null && pb != null) {
                            val dr = pa.first - pb.first
                            val dc = pa.second - pb.second
                            if (dr * dr + dc * dc <= 2.1) 0.35 else 1.0
                        } else {
                            1.0
                        }
                    }
                    var best = minOf(dp[i - 1][j] + 1.0, dp[i][j - 1] + 1.0, dp[i - 1][j - 1] + subCost)
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                        best = minOf(best, dp[i - 2][j - 2] + 0.65)
                    }
                    dp[i][j] = best
                }
            }
            return dp[m][n]
        }

        /**
         * Formats ranked candidates into Gboard's 3-slot strip order:
         * - When typing a non-empty prefix that differs from #1, places the quoted literal prefix
         *   on the left so users can tap to keep/learn custom words, with #1 centered.
         * - Otherwise places #1 in the highlighted center slot: [2nd, 1st, 3rd].
         */
        fun formatStripCandidates(
            prefix: String,
            candidates: List<SuggestionCandidate>,
        ): List<SuggestionCandidate> {
            val trimmed = prefix.trim()
            if (trimmed.isEmpty()) {
                return if (candidates.size >= 3) {
                    listOf(candidates[1], candidates[0], candidates[2])
                } else {
                    candidates
                }
            }

            if (candidates.isEmpty()) {
                return if (trimmed.length >= 2 && trimmed.all { it.isLetter() || it == '\'' || it == '-' }) {
                    listOf(SuggestionCandidate(trimmed, 0.0, isExact = true, isLiteral = true))
                } else {
                    emptyList()
                }
            }

            val top = candidates[0]
            if (top.word == trimmed) {
                return if (candidates.size >= 3) {
                    listOf(candidates[1], candidates[0], candidates[2])
                } else {
                    candidates
                }
            }

            val literal = SuggestionCandidate(trimmed, 0.0, isExact = true, isLiteral = true)
            val altRight = candidates.drop(1).firstOrNull { !it.word.equals(trimmed, ignoreCase = false) }
            return listOfNotNull(literal, top, altRight)
        }

        fun selectAutoReplacement(
            prefix: String,
            candidates: List<SuggestionCandidate>,
            mode: String,
            layoutId: String,
        ): SuggestionCandidate? {
            val trimmed = prefix.trim()
            if (trimmed.length < 2 || candidates.isEmpty()) return null
            if (!trimmed.all { it.isLetter() || it == '\'' || it == '-' }) return null

            // Never override a word that is already validly spelled in the dictionary.
            if (candidates.any { it.isExact && it.word.equals(trimmed, ignoreCase = true) }) {
                return null
            }

            val top = candidates.first()
            if (top.word.equals(trimmed, ignoreCase = true)) return null

            // 1. Clean accent restoration (e.g. "mogo" -> "môgô", "ecole" -> "école")
            if (top.isExact) {
                return top
            }

            // 2. Spatial/SymSpell typo fix in advanced_autocomplete mode (avoid expanding short stems into long unrelated words)
            if (mode == "advanced_autocomplete" && trimmed.length >= 3) {
                val cleanPrefix = stripAccents(trimmed.lowercase(Locale.ROOT))
                val cleanTop = stripAccents(top.word.lowercase(Locale.ROOT))
                if (abs(cleanTop.length - cleanPrefix.length) <= 1 &&
                    spatialDamerauDistance(cleanPrefix, cleanTop, layoutId) <= 1.05
                ) {
                    return top
                }
            }
            return null
        }

        fun parseDictionary(jsonText: String): DictionaryModel {
            val obj = JSONObject(jsonText)
            val wordsMap = mutableMapOf<String, Int>()
            obj.optJSONObject("words")?.let { wObj ->
                val keys = wObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    wordsMap[k] = wObj.optInt(k, 100)
                }
            }

            val bigramsMap = mutableMapOf<String, Map<String, Int>>()
            obj.optJSONObject("bigrams")?.let { bObj ->
                val keys = bObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val innerObj = bObj.optJSONObject(k) ?: continue
                    val innerMap = mutableMapOf<String, Int>()
                    val iKeys = innerObj.keys()
                    while (iKeys.hasNext()) {
                        val ik = iKeys.next()
                        innerMap[ik] = innerObj.optInt(ik, 100)
                    }
                    bigramsMap[k] = innerMap
                }
            }

            val trigramsMap = mutableMapOf<String, Map<String, Int>>()
            obj.optJSONObject("trigrams")?.let { tObj ->
                val keys = tObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val innerObj = tObj.optJSONObject(k) ?: continue
                    val innerMap = mutableMapOf<String, Int>()
                    val iKeys = innerObj.keys()
                    while (iKeys.hasNext()) {
                        val ik = iKeys.next()
                        innerMap[ik] = innerObj.optInt(ik, 100)
                    }
                    trigramsMap[k] = innerMap
                }
            }

            return DictionaryModel(wordsMap, bigramsMap, trigramsMap)
        }
    }
}
