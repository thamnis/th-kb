package com.thamnis.thkb.engine

import android.content.Context
import com.thamnis.thkb.data.PersonalDictionaryState
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.log10

data class KeyCenterPoint(
    val ch: String,
    val x: Float,
    val y: Float,
)

data class SwipeTrailPoint(
    val x: Float,
    val y: Float,
)

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

    @Volatile
    private var pendingDialectOverlayJson: String = ""

    @Volatile
    private var fallbackDialectIndex: IndexedFallbackModel? = null

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

    fun syncDialectOverlay(overlayJson: String) {
        pendingDialectOverlayJson = overlayJson
        fallbackDialectIndex = if (overlayJson.isBlank()) {
            null
        } else {
            buildIndexedFallback("", parseDictionary(overlayJson))
        }
        if (nativeReady) {
            NativeSuggestionBridge.nativeSyncDialects(overlayJson)
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
        val initialOverlay = pendingDialectOverlayJson
        nativeReady = NativeSuggestionBridge.nativeInitDictionaries(
            enTsv = enTsv,
            enCommonJson = enJson,
            frTsv = frTsv,
            frCommonJson = frJson,
            initialOverlayJson = initialOverlay,
        )
        if (nativeReady) {
            pendingPersonalJson?.let { json ->
                NativeSuggestionBridge.nativeSyncPersonalDictionary(json)
            }
            if (pendingDialectOverlayJson.isNotEmpty()) {
                NativeSuggestionBridge.nativeSyncDialects(pendingDialectOverlayJson)
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

    fun getActiveDictionary(layoutId: String, ivorianEnabled: Boolean = false): DictionaryModel {
        val isFr = layoutId.contains("fr") || layoutId.contains("ci")
        val baseKey = if (isFr) "fr_common" else "en_common"
        val base = loadDictionary(baseKey)
        return if (ivorianEnabled && isFr) {
            base.merge(loadDictionary("fr_ci_nouchi"))
        } else {
            base
        }
    }

    private fun getFallbackIndex(layoutId: String): IndexedFallbackModel {
        val isFr = layoutId.contains("fr") || layoutId.contains("ci")
        val cacheKey = if (isFr) "fr" else "en"
        return indexedCache.getOrPut(cacheKey) {
            val tsv = readAssetOrEmpty(if (isFr) "dictionaries/fr_words.txt" else "dictionaries/en_words.txt")
            val jsonModel = getActiveDictionary(layoutId, ivorianEnabled = false)
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
        val fallbackIdx = getFallbackIndex(layoutId)
        val combined = queryIndexed(
            index = fallbackIdx,
            contextWords = contextWords,
            prefix = prefix,
            mode = mode,
            layoutId = layoutId,
            capitalizeHint = capitalizeHint,
            maxResults = maxResults,
        ).toMutableList()

        val overlayIdx = fallbackDialectIndex ?: return combined
        val ovHits = queryIndexed(
            index = overlayIdx,
            contextWords = contextWords,
            prefix = prefix,
            mode = mode,
            layoutId = layoutId,
            capitalizeHint = capitalizeHint,
            maxResults = maxResults,
        )
        for (hit in ovHits) {
            val existingIdx = combined.indexOfFirst { it.word.equals(hit.word, ignoreCase = true) }
            if (existingIdx >= 0) {
                val prev = combined[existingIdx]
                combined[existingIdx] = prev.copy(
                    confidence = maxOf(prev.confidence, hit.confidence),
                    isExact = prev.isExact || hit.isExact,
                )
            } else {
                combined.add(hit)
            }
        }
        return combined.sortedByDescending { it.confidence }.take(maxResults)
    }

    /**
     * Decodes a glided swipe path over key centers into ranked word candidates.
     * Uses native Rust engine on device and a matching Kotlin geometric fallback in JVM tests.
     */
    fun decodeSwipePath(
        layoutId: String,
        contextWords: List<String>,
        keyCenters: List<KeyCenterPoint>,
        swipePoints: List<SwipeTrailPoint>,
        capitalizeHint: Boolean = false,
        maxResults: Int = 3,
    ): List<SuggestionCandidate> {
        if (keyCenters.isEmpty() || swipePoints.size < 2) return emptyList()

        if (ensureNativeInitialized()) {
            val centersJson = buildString {
                append('[')
                keyCenters.forEachIndexed { idx, kc ->
                    if (idx > 0) append(',')
                    append("{\"ch\":\"")
                    append(kc.ch.replace("\"", ""))
                    append("\",\"x\":")
                    append(kc.x)
                    append(",\"y\":")
                    append(kc.y)
                    append('}')
                }
                append(']')
            }
            val pointsJson = buildString {
                append('[')
                swipePoints.forEachIndexed { idx, pt ->
                    if (idx > 0) append(',')
                    append("{\"x\":")
                    append(pt.x)
                    append(",\"y\":")
                    append(pt.y)
                    append('}')
                }
                append(']')
            }
            val ctxJoined = contextWords.joinToString("\u001F")
            val json = NativeSuggestionBridge.nativeDecodeSwipe(
                layoutId = layoutId,
                contextJoined = ctxJoined,
                keyCentersJson = centersJson,
                swipePointsJson = pointsJson,
                capitalizeHint = capitalizeHint,
                maxResults = maxResults,
            )
            if (json.isNotEmpty()) {
                return parseNativeCandidates(json)
            }
        }

        val fallbackIdx = getFallbackIndex(layoutId)
        val mergedOverlay = fallbackDialectIndex
        return decodeSwipeFallback(
            base = fallbackIdx,
            overlay = mergedOverlay,
            contextWords = contextWords,
            keyCenters = keyCenters,
            swipePoints = swipePoints,
            capitalizeHint = capitalizeHint,
            maxResults = maxResults,
        )
    }

    internal fun decodeSwipeForDictionary(
        wordTsv: String,
        model: DictionaryModel,
        contextWords: List<String>,
        keyCenters: List<KeyCenterPoint>,
        swipePoints: List<SwipeTrailPoint>,
        capitalizeHint: Boolean = false,
        maxResults: Int = 3,
    ): List<SuggestionCandidate> {
        val index = buildIndexedFallback(wordTsv, model)
        return decodeSwipeFallback(
            base = index,
            overlay = fallbackDialectIndex,
            contextWords = contextWords,
            keyCenters = keyCenters,
            swipePoints = swipePoints,
            capitalizeHint = capitalizeHint,
            maxResults = maxResults,
        )
    }

    private fun decodeSwipeFallback(
        base: IndexedFallbackModel,
        overlay: IndexedFallbackModel?,
        contextWords: List<String>,
        keyCenters: List<KeyCenterPoint>,
        swipePoints: List<SwipeTrailPoint>,
        capitalizeHint: Boolean,
        maxResults: Int,
    ): List<SuggestionCandidate> {
        val centersMap = mutableMapOf<Char, Pair<Double, Double>>()
        for (kc in keyCenters) {
            val clean = stripAccents(kc.ch.lowercase(Locale.ROOT))
            val ch = clean.firstOrNull() ?: continue
            if (ch.isLetter()) {
                centersMap[ch] = kc.x.toDouble() to kc.y.toDouble()
            }
        }
        if (centersMap.isEmpty() || swipePoints.size < 2) return emptyList()

        val keyUnit = centersMap.entries.mapNotNull { (ca, pa) ->
            centersMap.entries
                .filter { it.key != ca }
                .minOfOrNull { (_, pb) -> hypot(pa.first - pb.first, pa.second - pb.second) }
        }.sorted().let { list ->
            if (list.isEmpty()) 80.0 else list[list.size / 2].coerceAtLeast(24.0)
        }

        val resampled = resampleSwipePath(swipePoints, 28) ?: return emptyList()
        val (pts, totalLen) = resampled
        if (totalLen < keyUnit * 0.45) return emptyList()

        val startPt = pts.first()
        val endPt = pts.last()
        val startChars = centersMap.entries
            .map { it.key to hypot(it.value.first - startPt.first, it.value.second - startPt.second) }
            .filter { it.second <= keyUnit * 1.4 }
            .sortedBy { it.second }
            .take(4)
            .map { it.first }
            .toSet()
        val endChars = centersMap.entries
            .map { it.key to hypot(it.value.first - endPt.first, it.value.second - endPt.second) }
            .filter { it.second <= keyUnit * 1.4 }
            .sortedBy { it.second }
            .take(4)
            .map { it.first }
            .toSet()
        if (startChars.isEmpty() || endChars.isEmpty()) return emptyList()

        val blockedSet = personalState.blocked
        val ranked = mutableMapOf<String, Pair<String, Double>>()
        fun evaluateEntries(idx: IndexedFallbackModel, overlayBoost: Double) {
            for (startCh in startChars) {
                val bucket = idx.prefix1[startCh] ?: continue
                for (entryIdx in bucket) {
                    val entry = idx.entries[entryIdx]
                    if (entry.clean.length < 2 || blockedSet.contains(entry.canonical.lowercase(Locale.ROOT))) continue
                    val lastCh = entry.clean.lastOrNull { it.isLetter() } ?: continue
                    if (lastCh !in endChars) continue
                    val score = scoreSwipeFallbackWord(
                        cleanWord = entry.clean,
                        logFreq = (entry.logFreq / 100.0) + overlayBoost,
                        centers = centersMap,
                        resampled = pts,
                        totalLen = totalLen,
                        keyUnit = keyUnit,
                    ) ?: continue
                    val lower = entry.canonical.lowercase(Locale.ROOT)
                    val prev = ranked[lower]
                    if (prev == null || score > prev.second) {
                        ranked[lower] = entry.canonical to score
                    }
                }
            }
        }

        evaluateEntries(base, 0.0)
        if (overlay != null) {
            evaluateEntries(overlay, 0.6)
        }

        val casing = if (capitalizeHint) CasingStyle.TITLE else CasingStyle.LOWER
        return ranked.values
            .sortedByDescending { it.second }
            .take(maxResults.coerceAtLeast(1))
            .map { (w, s) ->
                SuggestionCandidate(
                    word = applyCasing(w, casing),
                    confidence = s,
                    isExact = false,
                )
            }
    }

    private fun resampleSwipePath(
        raw: List<SwipeTrailPoint>,
        targetCount: Int,
    ): Pair<List<Pair<Double, Double>>, Double>? {
        if (raw.size < 2 || targetCount < 2) return null
        val cleaned = mutableListOf<Pair<Double, Double>>()
        for (pt in raw) {
            val p = pt.x.toDouble() to pt.y.toDouble()
            val last = cleaned.lastOrNull()
            if (last == null || hypot(p.first - last.first, p.second - last.second) >= 2.0) {
                cleaned.add(p)
            }
        }
        if (cleaned.size < 2) cleaned.add(raw.last().x.toDouble() to raw.last().y.toDouble())
        val cumLens = DoubleArray(cleaned.size)
        for (i in 1 until cleaned.size) {
            cumLens[i] = cumLens[i - 1] + hypot(
                cleaned[i].first - cleaned[i - 1].first,
                cleaned[i].second - cleaned[i - 1].second,
            )
        }
        val totalLen = cumLens.last()
        if (totalLen < 12.0) return null
        val out = ArrayList<Pair<Double, Double>>(targetCount)
        var segIdx = 0
        for (step in 0 until targetCount) {
            val targetD = (step.toDouble() / (targetCount - 1)) * totalLen
            while (segIdx + 1 < cumLens.size && cumLens[segIdx + 1] < targetD) segIdx++
            if (segIdx + 1 >= cleaned.size) {
                out.add(cleaned.last())
            } else {
                val span = (cumLens[segIdx + 1] - cumLens[segIdx]).coerceAtLeast(1e-6)
                val t = ((targetD - cumLens[segIdx]) / span).coerceIn(0.0, 1.0)
                val a = cleaned[segIdx]
                val b = cleaned[segIdx + 1]
                out.add((a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t))
            }
        }
        return out to totalLen
    }

    private fun scoreSwipeFallbackWord(
        cleanWord: String,
        logFreq: Double,
        centers: Map<Char, Pair<Double, Double>>,
        resampled: List<Pair<Double, Double>>,
        totalLen: Double,
        keyUnit: Double,
    ): Double? {
        val pts = mutableListOf<Pair<Double, Double>>()
        var prevCh: Char? = null
        for (ch in cleanWord) {
            if (!ch.isLetter() || ch == prevCh) continue
            val p = centers[ch] ?: return null
            pts.add(p)
            prevCh = ch
        }
        if (pts.size < 2) return null
        val startD = hypot(pts.first().first - resampled.first().first, pts.first().second - resampled.first().second)
        val endD = hypot(pts.last().first - resampled.last().first, pts.last().second - resampled.last().second)
        if (startD > keyUnit * 1.4 || endD > keyUnit * 1.4) return null

        val m = resampled.size
        var dp = DoubleArray(m) { Double.POSITIVE_INFINITY }
        for (j in 0 until minOf(3, m)) {
            dp[j] = hypot(pts[0].first - resampled[j].first, pts[0].second - resampled[j].second)
        }
        for (i in 1 until pts.size) {
            val next = DoubleArray(m) { Double.POSITIVE_INFINITY }
            var best = Double.POSITIVE_INFINITY
            for (j in 0 until m) {
                if (dp[j] < best) best = dp[j]
                if (best.isFinite()) {
                    next[j] = best + hypot(pts[i].first - resampled[j].first, pts[i].second - resampled[j].second)
                }
            }
            dp = next
        }
        val forwardSum = (maxOf(0, m - 3) until m).minOf { dp[it] }
        if (!forwardSum.isFinite()) return null
        val forwardAvg = forwardSum / pts.size
        if (forwardAvg > keyUnit * 1.4) return null

        var reverseSum = 0.0
        for (sp in resampled) {
            var minD = Double.POSITIVE_INFINITY
            for (i in 0 until pts.lastIndex) {
                val a = pts[i]
                val b = pts[i + 1]
                val abx = b.first - a.first
                val aby = b.second - a.second
                val ab2 = abx * abx + aby * aby
                val d = if (ab2 <= 1e-6) {
                    hypot(sp.first - a.first, sp.second - a.second)
                } else {
                    val t = (((sp.first - a.first) * abx + (sp.second - a.second) * aby) / ab2).coerceIn(0.0, 1.0)
                    hypot(sp.first - (a.first + t * abx), sp.second - (a.second + t * aby))
                }
                if (d < minD) minD = d
            }
            reverseSum += minD
        }
        val reverseAvg = reverseSum / m
        if (reverseAvg > keyUnit * 1.5) return null

        var idealLen = 0.0
        for (i in 0 until pts.lastIndex) {
            idealLen += hypot(pts[i + 1].first - pts[i].first, pts[i + 1].second - pts[i].second)
        }
        if (totalLen > idealLen * 2.1 + keyUnit * 1.2 || idealLen > totalLen * 2.1 + keyUnit * 1.2) {
            return null
        }
        val lenDiff = abs(totalLen - idealLen)
        return logFreq * 3.0 -
            (forwardAvg / keyUnit) * 24.0 -
            (reverseAvg / keyUnit) * 28.0 -
            ((startD + endD) / keyUnit) * 20.0 -
            (lenDiff / keyUnit) * 7.5
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
