package com.thamnis.thkb.engine

enum class FancyFontStyle(
    val id: String,
    val displayName: String,
    val sample: String,
) {
    NORMAL("normal", "Normal", "Abc"),
    BOLD_SERIF("bold_serif", "𝐁𝐨𝐥𝐝", "𝐀𝐛𝐜"),
    ITALIC_SERIF("italic_serif", "𝐼𝑡𝑎𝑙𝑖𝑐", "𝐴𝑏𝑐"),
    BOLD_SCRIPT("bold_script", "𝓢𝓬𝓻𝓲𝓹𝓽", "𝓐𝓫𝓬"),
    FRAKTUR("fraktur", "𝔉𝔯𝔞𝔨𝔱𝔲𝔯", "𝔄𝔟𝔠"),
    DOUBLE_STRUCK("double_struck", "𝔻𝕠𝕦𝕓𝕝𝕖", "𝔸𝕓𝕔"),
    MONOSPACE("monospace", "𝙼𝚘𝚗𝚘", "𝙰𝚋𝚌"),
    CIRCLED("circled", "Ⓒⓘⓡⓒⓛⓔ", "Ⓐⓑⓒ"),
    SMALL_CAPS("small_caps", "Sᴍᴀʟʟ Cᴀᴘs", "Aʙᴄ"),
    FULLWIDTH("fullwidth", "Ｗｉｄｅ", "Ａｂｃ"),
}

object FancyTextTransformer {
    private val smallCapsMap = mapOf(
        'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ", 'f' to "ғ",
        'g' to "ɢ", 'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ", 'k' to "ᴋ", 'l' to "ʟ",
        'm' to "ᴍ", 'n' to "ɴ", 'o' to "ᴏ", 'p' to "ᴘ", 'q' to "ǫ", 'r' to "ʀ",
        's' to "s", 't' to "ᴛ", 'u' to "ᴜ", 'v' to "ᴠ", 'w' to "ᴡ", 'x' to "x",
        'y' to "ʏ", 'z' to "ᴢ",
    )

    private val frakturExceptions = mapOf(
        'C' to "ℭ", 'H' to "ℌ", 'I' to "ℑ", 'R' to "ℜ", 'Z' to "ℨ",
    )

    private val doubleStruckExceptions = mapOf(
        'C' to "ℂ", 'H' to "ℍ", 'N' to "ℕ", 'P' to "ℙ", 'Q' to "ℚ", 'R' to "ℝ", 'Z' to "ℤ",
    )

    private val italicExceptions = mapOf(
        'h' to "ℎ",
    )

    private fun codePointStr(cp: Int): String = String(Character.toChars(cp))

    private fun transformOffsetChar(
        ch: Char,
        upperStartCp: Int,
        lowerStartCp: Int,
        digitStartCp: Int? = null,
        exceptions: Map<Char, String> = emptyMap(),
    ): String {
        exceptions[ch]?.let { return it }
        return when (ch) {
            in 'A'..'Z' -> codePointStr(upperStartCp + (ch - 'A'))
            in 'a'..'z' -> codePointStr(lowerStartCp + (ch - 'a'))
            in '0'..'9' -> if (digitStartCp != null) codePointStr(digitStartCp + (ch - '0')) else ch.toString()
            else -> ch.toString()
        }
    }

    fun transform(text: String, style: FancyFontStyle): String {
        if (style == FancyFontStyle.NORMAL || text.isEmpty()) return text
        val out = StringBuilder(text.length * 2)
        for (ch in text) {
            val mapped = when (style) {
                FancyFontStyle.NORMAL -> ch.toString()
                FancyFontStyle.BOLD_SERIF -> transformOffsetChar(ch, 0x1D400, 0x1D41A, 0x1D7CE)
                FancyFontStyle.ITALIC_SERIF -> transformOffsetChar(ch, 0x1D434, 0x1D44E, exceptions = italicExceptions)
                FancyFontStyle.BOLD_SCRIPT -> transformOffsetChar(ch, 0x1D4D0, 0x1D4EA)
                FancyFontStyle.FRAKTUR -> transformOffsetChar(ch, 0x1D504, 0x1D51E, exceptions = frakturExceptions)
                FancyFontStyle.DOUBLE_STRUCK -> transformOffsetChar(ch, 0x1D538, 0x1D552, 0x1D7D8, doubleStruckExceptions)
                FancyFontStyle.MONOSPACE -> transformOffsetChar(ch, 0x1D670, 0x1D68A, 0x1D7F6)
                FancyFontStyle.CIRCLED -> when (ch) {
                    in 'A'..'Z' -> codePointStr(0x24B6 + (ch - 'A'))
                    in 'a'..'z' -> codePointStr(0x24D0 + (ch - 'a'))
                    '0' -> "⓪"
                    in '1'..'9' -> codePointStr(0x2460 + (ch - '1'))
                    else -> ch.toString()
                }
                FancyFontStyle.SMALL_CAPS -> smallCapsMap[ch] ?: ch.toString()
                FancyFontStyle.FULLWIDTH -> when {
                    ch == ' ' -> "\u3000"
                    ch in '!'..'~' -> codePointStr(0xFF01 + (ch - '!'))
                    else -> ch.toString()
                }
            }
            out.append(mapped)
        }
        return out.toString()
    }
}
