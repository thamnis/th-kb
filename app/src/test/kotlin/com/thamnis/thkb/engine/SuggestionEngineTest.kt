package com.thamnis.thkb.engine

import com.thamnis.thkb.data.DialectPackManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    private val sampleJson = """
        {
          "words": {
            "bonjour": 120000,
            "bon": 180000,
            "je": 8300000,
            "est": 6900000,
            "tu": 4400000,
            "môgô": 80000,
            "djossi": 75000,
            "enjaillement": 72000,
            "sciencer": 70000,
            "ya": 60000,
            "fohi": 67000,
            "Abidjan": 90000,
            "chap-chap": 75000
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
        assertEquals(13, dict.words.size)
        assertEquals(120000, dict.words["bonjour"])
        assertEquals(990, dict.bigrams["ya"]?.get("fohi"))
        assertEquals(980, dict.trigrams["il faut"]?.get("sciencer"))
    }

    @Test
    fun cleanStartReturnsEmptyInsteadOfStopwords() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        val candidates = engine.querySuggestions(
            dict = dict,
            contextWords = emptyList(),
            prefix = "",
            mode = "advanced_autocomplete",
            maxResults = 3,
            capitalizeHint = true,
        )
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun matchesLowercaseAndDisplaysCorrectCasing() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        val lower = engine.querySuggestions(dict, emptyList(), "bonj", "autocomplete", 3)
        assertEquals("bonjour", lower[0].word)

        val title = engine.querySuggestions(dict, emptyList(), "Bonj", "autocomplete", 3)
        assertEquals("Bonjour", title[0].word)

        val autoCap = engine.querySuggestions(dict, emptyList(), "bonj", "autocomplete", 3, capitalizeHint = true)
        assertEquals("Bonjour", autoCap[0].word)

        val caps = engine.querySuggestions(dict, emptyList(), "BONJ", "autocomplete", 3)
        assertEquals("BONJOUR", caps[0].word)

        val city = engine.querySuggestions(dict, emptyList(), "abidj", "autocomplete", 3)
        assertEquals("Abidjan", city[0].word)
    }

    @Test
    fun symSpellAndSpatialFixesAdjacentTypo() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        val candidates = engine.querySuggestions(
            dict = dict,
            contextWords = emptyList(),
            prefix = "bpnjour",
            mode = "advanced_autocomplete",
            maxResults = 3,
            layoutId = "azerty_fr",
        )
        assertFalse(candidates.isEmpty())
        assertEquals("bonjour", candidates[0].word)
    }

    @Test
    fun extractContextSweetSpotWindow() {
        val engine = SuggestionEngine(null)

        val (ctx1, prefix1) = engine.extractContext("Salut mon ami ")
        assertEquals(listOf("mon", "ami"), ctx1)
        assertEquals("", prefix1)

        val (ctx2, prefix2) = engine.extractContext("il faut scie")
        assertEquals(listOf("il", "faut"), ctx2)
        assertEquals("scie", prefix2)

        val (ctx3, prefix3) = engine.extractContext("bonj")
        assertEquals(emptyList<String>(), ctx3)
        assertEquals("bonj", prefix3)
    }

    @Test
    fun querySuggestionsPrefixAndAccents() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

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
    fun querySuggestionsAdvancedTrigramAndBigramContext() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        val tri = engine.querySuggestions(
            dict = dict,
            contextWords = listOf("il", "faut"),
            prefix = "",
            mode = "advanced_autocomplete",
            maxResults = 3,
        )
        assertFalse(tri.isEmpty())
        assertEquals("sciencer", tri[0].word)

        val bi = engine.querySuggestions(
            dict = dict,
            contextWords = listOf("ya"),
            prefix = "",
            mode = "advanced_autocomplete",
            maxResults = 3,
        )
        assertFalse(bi.isEmpty())
        assertEquals("fohi", bi[0].word)
    }

    @Test
    fun autoReplacementRestoresAccentsAndFixesTyposWithoutHijackingValidWords() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        // Missing accents: "mogo" -> "môgô"
        val mogoCandidates = engine.querySuggestions(dict, emptyList(), "mogo", "advanced_autocomplete", 5)
        assertEquals("môgô", SuggestionEngine.selectAutoReplacement("mogo", mogoCandidates, "advanced_autocomplete", "azerty_fr")?.word)

        // Spatial typo: "bpnjour" -> "bonjour"
        val typoCandidates = engine.querySuggestions(dict, emptyList(), "bpnjour", "advanced_autocomplete", 5)
        assertEquals("bonjour", SuggestionEngine.selectAutoReplacement("bpnjour", typoCandidates, "advanced_autocomplete", "azerty_fr")?.word)

        // Valid word already in dict ("bon") is never hijacked
        val validCandidates = engine.querySuggestions(dict, emptyList(), "bon", "advanced_autocomplete", 5)
        assertEquals(null, SuggestionEngine.selectAutoReplacement("bon", validCandidates, "advanced_autocomplete", "azerty_fr"))

        // Short prefix fragment ("bonj") or unrelated custom name ("Zahi") is not blindly expanded on space
        val partialCandidates = engine.querySuggestions(dict, emptyList(), "bonj", "advanced_autocomplete", 5)
        assertEquals(null, SuggestionEngine.selectAutoReplacement("bonj", partialCandidates, "advanced_autocomplete", "azerty_fr"))
    }

    @Test
    fun personalDictionaryLearnsCustomWordsBigramsAndBlocks() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        engine.learnWord("bonjour", "Thamnis", weightBoost = 15)

        val customMatches = engine.querySuggestions(dict, emptyList(), "tham", "advanced_autocomplete", 3)
        assertFalse(customMatches.isEmpty())
        assertEquals("Thamnis", customMatches[0].word)

        val learnedNext = engine.querySuggestions(dict, listOf("Bonjour"), "", "advanced_autocomplete", 3)
        assertFalse(learnedNext.isEmpty())
        assertEquals("Thamnis", learnedNext[0].word)

        engine.blockWord("bonjour")
        val blockedCheck = engine.querySuggestions(dict, emptyList(), "bonj", "autocomplete", 3)
        assertFalse(blockedCheck.any { it.word == "bonjour" })
    }

    @Test
    fun formatStripCandidatesPlacesLiteralLeftAndPrimaryCenter() {
        val dict = SuggestionEngine.parseDictionary(sampleJson)
        val engine = SuggestionEngine(null)

        // When typing "bonj", literal "bonj" sits left and "bonjour" sits center
        val partialRaw = engine.querySuggestions(dict, emptyList(), "bonj", "advanced_autocomplete", 3)
        val partialStrip = SuggestionEngine.formatStripCandidates("bonj", partialRaw)
        assertEquals("bonj", partialStrip[0].word)
        assertEquals(true, partialStrip[0].isLiteral)
        assertEquals("bonjour", partialStrip[1].word)
        assertEquals(false, partialStrip[1].isLiteral)

        // Unknown word surfaces literal chip so user can tap to keep/learn it
        val unknownStrip = SuggestionEngine.formatStripCandidates("Thamnis", emptyList())
        assertEquals(1, unknownStrip.size)
        assertEquals("Thamnis", unknownStrip[0].word)
        assertEquals(true, unknownStrip[0].isLiteral)
    }

    @Test
    fun dialectPackTemplateParsesAndOverlayMerges() {
        val templateJson = DialectPackManager.createStarterTemplate()
        val parsed = DialectPackManager.parsePack(templateJson, isBuiltIn = false)
        assertNotNull(parsed)
        assertEquals("My Local Jargon", parsed?.name)
        assertEquals(2, parsed?.wordCount)

        val engine = SuggestionEngine(null)
        engine.syncDialectOverlay(templateJson)
        val hits = engine.suggestForInput(
            layoutId = "qwerty_en",
            contextWords = emptyList(),
            prefix = "exampl",
            mode = "advanced_autocomplete",
            ivorianEnabled = false,
            capitalizeHint = false,
            maxResults = 3,
        )
        assertFalse(hits.isEmpty())
        assertEquals("exampleword", hits[0].word)
    }

    @Test
    fun bundledDialectAssetsParseCleanly() {
        val dir = java.io.File("src/main/assets/dialects").takeIf { it.isDirectory }
            ?: java.io.File("app/src/main/assets/dialects")
        val files = dir.listFiles { file -> file.extension == "json" }?.sortedBy { it.name }.orEmpty()
        assertEquals(14, files.size)
        val seenIds = mutableSetOf<String>()
        files.forEach { file ->
            val parsed = DialectPackManager.parsePack(file.readText(), isBuiltIn = true)
            assertNotNull("Failed to parse ${file.name}", parsed)
            assertFalse("Empty word list in ${file.name}", parsed!!.wordCount == 0)
            assertFalse("Duplicate id ${parsed.id}", !seenIds.add(parsed.id))
        }
    }
}
