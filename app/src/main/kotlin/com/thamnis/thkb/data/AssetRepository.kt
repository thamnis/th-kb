package com.thamnis.thkb.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class KaomojiItem(
    val text: String,
    val category: String,
    val tags: List<String>,
)

data class EmojiSection(
    val category: String,
    val emojis: List<String>,
)

class AssetRepository(private val context: Context) {

    private val layoutCache = mutableMapOf<String, List<List<String>>>()
    private var accentsCache: Map<String, List<String>>? = null
    private var kaomojiCache: List<KaomojiItem>? = null
    private var emojiSectionsCache: List<EmojiSection>? = null

    fun loadLayout(layoutId: String): List<List<String>> {
        layoutCache[layoutId]?.let { return it }
        val fileName = when (layoutId) {
            "numeric" -> "layouts/numeric.json"
            "azerty_fr" -> "layouts/azerty_fr.json"
            "symbols" -> "layouts/symbols.json"
            "symbols_more" -> "layouts/symbols_more.json"
            else -> "layouts/qwerty_en.json"
        }
        val jsonText = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val rowsArray = root.getJSONArray("rows")
        val rows = buildList {
            for (i in 0 until rowsArray.length()) {
                val rowJson = rowsArray.getJSONArray(i)
                add(
                    buildList {
                        for (j in 0 until rowJson.length()) {
                            add(rowJson.getString(j))
                        }
                    },
                )
            }
        }
        layoutCache[layoutId] = rows
        return rows
    }

    fun lookupAccents(baseKey: String): List<String> {
        val map = accentsCache ?: loadAccents().also { accentsCache = it }
        val lower = baseKey.lowercase()
        val variants = map[lower] ?: return emptyList()
        return if (baseKey.length == 1 && baseKey[0].isUpperCase()) {
            variants.map { it.uppercase() }
        } else {
            variants
        }
    }

    @Synchronized
    fun loadKaomoji(): List<KaomojiItem> {
        kaomojiCache?.let { return it }
        val jsonText = context.assets.open("kaomoji/kaomoji.json").bufferedReader().use { it.readText() }
        val array = JSONArray(jsonText)
        val items = buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tagsJson = obj.getJSONArray("tags")
                val tags = buildList {
                    for (t in 0 until tagsJson.length()) add(tagsJson.getString(t))
                }
                add(
                    KaomojiItem(
                        text = obj.getString("text"),
                        category = obj.getString("category"),
                        tags = tags,
                    ),
                )
            }
        }
        kaomojiCache = items
        return items
    }

    @Synchronized
    fun loadEmojiSections(): List<EmojiSection> {
        emojiSectionsCache?.let { return it }
        val jsonText = context.assets.open("kaomoji/emojis.json").bufferedReader().use { it.readText() }
        val array = JSONArray(jsonText)
        val sections = buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val emojisJson = obj.getJSONArray("emojis")
                val emojis = buildList {
                    for (e in 0 until emojisJson.length()) add(emojisJson.getString(e))
                }
                add(
                    EmojiSection(
                        category = obj.getString("category"),
                        emojis = emojis,
                    ),
                )
            }
        }
        emojiSectionsCache = sections
        return sections
    }

    private fun loadAccents(): Map<String, List<String>> {
        val jsonText = context.assets.open("layouts/accents.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val result = mutableMapOf<String, List<String>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val arr = root.getJSONArray(key)
            result[key] = buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        }
        return result
    }
}
