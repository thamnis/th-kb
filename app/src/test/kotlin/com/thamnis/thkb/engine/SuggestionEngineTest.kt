package com.thamnis.thkb.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SuggestionEngineTest {

    private val sampleJson = """
        {
          "words": {
            "bonjour": 1200,
            "bon": 1000,
            "môgô": 950,
            "djossi": 920,
            "enjaillement": 910,
            "sciencer": 900,
            "ya": 800,
            "fohi": 870,
            "chap": 860,
            "chap-chap": 920
          },
          "bigrams": {
            "ya": { "fohi": 990 },
            "chap": { "chap": 990 }
          },
          "trigrams": {
            "il faut": { "sciencer": 980 }
          }
        }
    """.trimIndent()

    @Test
    fun parsesDictionaryCorrectly() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        assertEquals(10, dict.words.size)
        assertEquals(1200, dict.words["bonjour"])
        assertEquals(990, dict.bigrams["ya"]?.get("fohi"))
        assertEquals(980, dict.trigrams["il faut"]?.get("sciencer"))
    }

    @Test
    fun extractContextSweetSpotWindow() {
        val engine = SuggestionEngine(null)

        // Test with trailing space (expect context words, empty prefix)
        val (ctx1, prefix1) = engine.extractContext("Salut mon ami ")
        assertEquals(listOf("mon", "ami"), ctx1)
        assertEquals("", prefix1)

        // Test with incomplete word (expect context words and prefix)
        val (ctx2, prefix2) = engine.extractContext("il faut scie")
        assertEquals(listOf("il", "faut"), ctx2)
        assertEquals("scie", prefix2)

        // Test with single word
        val (ctx3, prefix3) = engine.extractContext("bonj")
        assertEquals(emptyList<String>(), ctx3)
        assertEquals("bonj", prefix3)
    }

    @Test
    fun querySuggestionsPrefixAndAccents() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        // Accent insensitive search: "mogo" -> "môgô"
        val candidates = engine.querySuggestions(
            dict = dict,
            contextWords = emptyList(),
            prefix = "mogo",
            mode = "autocomplete",
            maxResults = 3,
        )
        assertFalse(candidates.isEmpty())
        assertEquals("môgô", candidates[0].word)
    }

    @Test
    fun querySuggestionsAdvancedTrigramContext() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        val candidates = engine.querySuggestions(
            dict = dict,
            contextWords = listOf("il", "faut"),
            prefix = "",
            mode = "advanced_autocomplete",
            maxResults = 3,
        )
        assertFalse(candidates.isEmpty())
        assertEquals("sciencer", candidates[0].word)
    }

    @Test
    fun querySuggestionsAdvancedBigramContext() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        val candidates = engine.querySuggestions(
            dict = dict,
            contextWords = listOf("ya"),
            prefix = "",
            mode = "advanced_autocomplete",
            maxResults = 3,
        )
        assertFalse(candidates.isEmpty())
        assertEquals("fohi", candidates[0].word)
    }
}
