package com.thamnis.thkb.engine

import android.content.Context
import org.json.JSONObject
import java.util.Locale

data class SuggestionCandidate(
    val word: String,
    val confidence: Double,
    val isExact: Boolean,
)

data class DictionaryModel(
    val words: Map<String, Int> = emptyMap(),
    val bigrams: Map<String, Map<String, Int>> = emptyMap(),
    val trigrams: Map<String, Map<String, Int>> = emptyMap(),
) {
    fun merge(other: DictionaryModel): DictionaryModel {
        val mergedWords = words.toMutableMap()
        for ((k, v) in other.words) {
            mergedWords[k] = (mergedWords[k] ?: 0) + v
        }

        val mergedBigrams = bigrams.mapValues { it.value.toMutableMap() }.toMutableMap()
        for ((k, nextMap) in other.bigrams) {
            val inner = mergedBigrams.getOrPut(k) { mutableMapOf() }
            for ((nextW, score) in nextMap) {
                inner[nextW] = (inner[nextW] ?: 0) + score
            }
        }

        val mergedTrigrams = trigrams.mapValues { it.value.toMutableMap() }.toMutableMap()
        for ((k, nextMap) in other.trigrams) {
            val inner = mergedTrigrams.getOrPut(k) { mutableMapOf() }
            for ((nextW, score) in nextMap) {
                inner[nextW] = (inner[nextW] ?: 0) + score
            }
        }

        return DictionaryModel(mergedWords, mergedBigrams, mergedTrigrams)
    }
}

class SuggestionEngine(private val context: Context? = null) {
    private val cache = mutableMapOf<String, DictionaryModel>()

    fun loadDictionary(name: String): DictionaryModel {
        val ctx = context ?: return DictionaryModel()
        return cache.getOrPut(name) {
            runCatching {
                val jsonText = ctx.assets.open("dictionaries/$name.json").bufferedReader().use { it.readText() }
                parseDictionary(jsonText)
            }.getOrDefault(DictionaryModel())
        }
    }

    fun getActiveDictionary(layoutId: String, ivorianEnabled: Boolean): DictionaryModel {
        val base = if (layoutId.contains("fr")) {
            loadDictionary("fr_common")
        } else {
            loadDictionary("en_common")
        }

        return if (ivorianEnabled && (layoutId.contains("fr") || layoutId.contains("ci"))) {
            base.merge(loadDictionary("fr_ci_nouchi"))
        } else {
            base
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
     * Queries suggestions according to mode: "autocomplete" or "advanced_autocomplete".
     */
    fun querySuggestions(
        dict: DictionaryModel,
        contextWords: List<String>,
        prefix: String,
        mode: String,
        maxResults: Int = 3,
    ): List<SuggestionCandidate> {
        val isAdvanced = mode == "advanced_autocomplete"
        val normPrefix = prefix.trim().lowercase(Locale.ROOT)
        val cleanPrefix = stripAccents(normPrefix)

        val scored = mutableListOf<Triple<String, Double, Boolean>>()

        if (normPrefix.isEmpty()) {
            // Next-word prediction (Trigram sweet-spot -> Bigram -> Top Unigrams)
            if (isAdvanced && contextWords.size >= 2) {
                val lastTwo = "${contextWords[contextWords.size - 2].lowercase(Locale.ROOT)} ${contextWords[contextWords.size - 1].lowercase(Locale.ROOT)}"
                dict.trigrams[lastTwo]?.forEach { (word, score) ->
                    scored.add(Triple(word, score.toDouble() * 3.0, false))
                }
            }

            if (scored.size < maxResults && contextWords.isNotEmpty()) {
                val prev = contextWords.last().lowercase(Locale.ROOT)
                dict.bigrams[prev]?.forEach { (word, score) ->
                    if (scored.none { it.first == word }) {
                        scored.add(Triple(word, score.toDouble() * 2.0, false))
                    }
                }
            }

            if (scored.size < maxResults) {
                dict.words.entries
                    .sortedByDescending { it.value }
                    .take(maxResults * 2)
                    .forEach { (word, score) ->
                        if (scored.none { it.first == word }) {
                            scored.add(Triple(word, score.toDouble() * 0.5, false))
                        }
                    }
            }
        } else {
            // Prefix search + contextual boosting
            for ((word, count) in dict.words) {
                val wordLower = word.lowercase(Locale.ROOT)
                val wordClean = stripAccents(wordLower)

                val isExact = wordLower == normPrefix
                val startsExact = wordLower.startsWith(normPrefix)
                val startsClean = wordClean.startsWith(cleanPrefix)

                if (startsExact || startsClean) {
                    var score = count.toDouble()
                    if (isExact) {
                        score += 5000.0
                    } else if (startsExact) {
                        score += 2000.0
                    } else {
                        score += 1500.0
                    }

                    // Favor words closer in length to typed prefix
                    val lenDiff = (wordLower.length - normPrefix.length).coerceAtLeast(0)
                    score -= lenDiff * 10.0

                    // Trigram / Bigram context boost in advanced mode
                    if (isAdvanced && contextWords.size >= 2) {
                        val lastTwo = "${contextWords[contextWords.size - 2].lowercase(Locale.ROOT)} ${contextWords[contextWords.size - 1].lowercase(Locale.ROOT)}"
                        dict.trigrams[lastTwo]?.get(word)?.let { triScore ->
                            score += triScore.toDouble() * 4.0
                        }
                    }

                    if (isAdvanced && contextWords.isNotEmpty()) {
                        val prev = contextWords.last().lowercase(Locale.ROOT)
                        dict.bigrams[prev]?.get(word)?.let { biScore ->
                            score += biScore.toDouble() * 2.5
                        }
                    }

                    scored.add(Triple(word, score, isExact))
                } else if (isAdvanced && normPrefix.length >= 3) {
                    // Typo tolerance (Levenshtein distance <= 1)
                    val targetPrefix = wordClean.take(cleanPrefix.length + 1)
                    if (editDistance(cleanPrefix, targetPrefix) <= 1) {
                        scored.add(Triple(word, count.toDouble() * 0.4, false))
                    }
                }
            }
        }

        // Sort descending by score and pick top unique
        scored.sortByDescending { it.second }

        val results = mutableListOf<SuggestionCandidate>()
        for ((word, score, isExact) in scored) {
            if (results.none { it.word.equals(word, ignoreCase = true) }) {
                results.add(SuggestionCandidate(word, score, isExact))
                if (results.size >= maxResults) break
            }
        }

        return results
    }

    companion object {
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
                    else -> out.append(c)
                }
            }
            return out.toString()
        }

        fun editDistance(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j

            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    val cost = if (a[i - 1].lowercaseChar() == b[j - 1].lowercaseChar()) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                }
            }
            return dp[a.length][b.length]
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
