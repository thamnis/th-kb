package com.thamnis.thkb.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Immutable

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK,
}

enum class KeyboardLayer {
    ALPHA,
    SYMBOLS,
    SYMBOLS_MORE,
    NUMERIC,
}

enum class PanelMode {
    KEYBOARD,
    EMOJI,
    KAOMOJI,
    CLIPBOARD,
    TODO,
    VOICE,
    NAV,
    MEDIA,
    REMOTE,
    SUITE,
}

enum class NavUnit {
    CHAR,
    WORD,
    PHRASE,
    PARAGRAPH,
    LINE,
    DOCUMENT,
}

enum class NavDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    START,
    END,
}

enum class MediaControlAction {
    PLAY_PAUSE,
    PREVIOUS,
    NEXT,
    REWIND,
    FAST_FORWARD,
    VOL_DOWN,
    VOL_UP,
    MUTE,
}

enum class InputContext {
    NORMAL,
    URL,
    EMAIL,
    NUMBER,
    PASSWORD,
    TERMINAL,
}

enum class ReturnAction(val label: String) {
    DEFAULT("↵"),
    GO("Go"),
    SEARCH("Search"),
    SEND("Send"),
    NEXT("Next"),
    DONE("Done"),
}

@Immutable
data class AccentPopupState(
    val baseKey: String,
    val variants: List<String>,
    val selectedIndex: Int = 0,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
)

@Immutable
data class KeyPreviewState(
    val label: String,
    val centerX: Float,
    val topY: Float,
)

object KeyboardStateLogic {
    fun nextShiftOnTap(current: ShiftState, isDoubleTap: Boolean = false): ShiftState = when {
        isDoubleTap && current == ShiftState.ONCE -> ShiftState.CAPS_LOCK
        current == ShiftState.OFF -> ShiftState.ONCE
        current == ShiftState.ONCE -> ShiftState.OFF
        else -> ShiftState.OFF
    }

    fun nextShiftOnLongPress(current: ShiftState): ShiftState = when (current) {
        ShiftState.CAPS_LOCK -> ShiftState.OFF
        ShiftState.OFF, ShiftState.ONCE -> ShiftState.CAPS_LOCK
    }

    fun shiftAfterCharacterCommit(current: ShiftState): ShiftState = when (current) {
        ShiftState.ONCE -> ShiftState.OFF
        ShiftState.OFF, ShiftState.CAPS_LOCK -> current
    }

    fun resolveKeyLabel(rawKey: String, shiftState: ShiftState, returnAction: ReturnAction): String =
        when (rawKey) {
            "SHIFT" -> when (shiftState) {
                ShiftState.OFF -> "⇧"
                ShiftState.ONCE -> "⬆"
                ShiftState.CAPS_LOCK -> "⇪"
            }
            "BACKSPACE" -> "⌫"
            "SYMBOLS" -> "?123"
            "MORE_SYMBOLS" -> "=\\<"
            "ALPHA" -> "ABC"
            "NUMERIC" -> "123"
            "EMOJI_KEY" -> "☺"
            "SPACE" -> "EN • FR"
            "RETURN" -> returnAction.label
            "'" -> if (shiftState != ShiftState.OFF) "?" else "'"
            else -> if (shiftState != ShiftState.OFF && rawKey.length == 1 && rawKey[0].isLetter()) {
                rawKey.uppercase()
            } else {
                rawKey
            }
        }

    private val qwertyHints = mapOf(
        "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
        "a" to "@", "s" to "#", "d" to "$", "f" to "_", "g" to "&",
        "h" to "-", "j" to "+", "k" to "(", "l" to ")",
        "z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";",
        "n" to "!", "m" to "?",
    )

    private val azertyHints = mapOf(
        "a" to "1", "z" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0",
        "q" to "@", "s" to "#", "d" to "$", "f" to "_", "g" to "&",
        "h" to "-", "j" to "+", "k" to "(", "l" to ")", "m" to "?",
        "w" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";",
        "n" to "!", "'" to "?",
    )

    fun hintForKey(layoutId: String, rawKey: String): String? =
        if (layoutId == "azerty_fr") azertyHints[rawKey.lowercase()] else qwertyHints[rawKey.lowercase()]

    fun resolveReturnAction(imeOptions: Int): ReturnAction {
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (noEnterAction) return ReturnAction.DEFAULT

        return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> ReturnAction.GO
            EditorInfo.IME_ACTION_SEARCH -> ReturnAction.SEARCH
            EditorInfo.IME_ACTION_SEND -> ReturnAction.SEND
            EditorInfo.IME_ACTION_NEXT -> ReturnAction.NEXT
            EditorInfo.IME_ACTION_DONE -> ReturnAction.DONE
            else -> ReturnAction.DEFAULT
        }
    }

    private val terminalPackages = setOf(
        "com.termux",
        "com.sonelli.juicessh",
        "jackpal.androidterm",
        "org.connectbot",
    )

    fun resolveInputContext(inputType: Int, packageName: String?): InputContext {
        if (packageName != null && packageName in terminalPackages) {
            return InputContext.TERMINAL
        }
        val klass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        if (klass == InputType.TYPE_CLASS_NUMBER ||
            klass == InputType.TYPE_CLASS_PHONE ||
            klass == InputType.TYPE_CLASS_DATETIME
        ) {
            return InputContext.NUMBER
        }

        if (klass == InputType.TYPE_CLASS_TEXT) {
            return when (variation) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                -> InputContext.PASSWORD
                InputType.TYPE_TEXT_VARIATION_URI -> InputContext.URL
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                -> InputContext.EMAIL
                else -> InputContext.NORMAL
            }
        }
        return InputContext.NORMAL
    }

    fun shouldAutoCapOnOpen(context: InputContext, autoCapPref: Boolean): Boolean =
        autoCapPref && context == InputContext.NORMAL

    fun shouldAutoCapBeforeCursor(
        textBeforeCursor: String,
        context: InputContext,
        autoCapPref: Boolean,
    ): Boolean {
        if (!autoCapPref || context != InputContext.NORMAL) return false
        if (textBeforeCursor.isEmpty() || textBeforeCursor.endsWith("\n")) return true
        val trimmed = textBeforeCursor.trimEnd(' ', '\t')
        if (trimmed.length == textBeforeCursor.length) return false
        val lastChar = trimmed.lastOrNull() ?: return true
        return lastChar == '.' || lastChar == '!' || lastChar == '?'
    }

    fun initialLayerForContext(context: InputContext): KeyboardLayer =
        if (context == InputContext.NUMBER) KeyboardLayer.NUMERIC else KeyboardLayer.ALPHA

    fun wordDeleteCountBefore(before: String): Int {
        if (before.isEmpty()) return 0
        var idx = before.length
        while (idx > 0 && before[idx - 1].isWhitespace()) idx--
        if (idx > 0 && !before[idx - 1].isLetterOrDigit()) {
            while (idx > 0 && !before[idx - 1].isLetterOrDigit() && !before[idx - 1].isWhitespace()) idx--
        } else {
            while (idx > 0 && before[idx - 1].isLetterOrDigit()) idx--
        }
        return (before.length - idx).coerceAtLeast(1)
    }

    fun wordDeleteCountAfter(after: String): Int {
        if (after.isEmpty()) return 0
        var idx = 0
        while (idx < after.length && after[idx].isWhitespace()) idx++
        if (idx < after.length && !after[idx].isLetterOrDigit()) {
            while (idx < after.length && !after[idx].isLetterOrDigit() && !after[idx].isWhitespace()) idx++
        } else {
            while (idx < after.length && after[idx].isLetterOrDigit()) idx++
        }
        return idx.coerceAtLeast(1)
    }

    fun paragraphDeleteCountBefore(before: String): Int {
        if (before.isEmpty()) return 0
        val trimmedEnd = if (before.endsWith("\n")) before.length - 1 else before.length
        val prevNewline = before.lastIndexOf('\n', startIndex = (trimmedEnd - 1).coerceAtLeast(0))
        return if (prevNewline < 0) before.length else (before.length - prevNewline - 1)
    }

    fun paragraphDeleteCountAfter(after: String): Int {
        if (after.isEmpty()) return 0
        val start = if (after.startsWith("\n")) 1 else 0
        val nextNewline = after.indexOf('\n', startIndex = start)
        return if (nextNewline < 0) after.length else (nextNewline + 1)
    }

    /** Cycles selected text through lower -> Title Case -> UPPERCASE -> lower. */
    fun cycleSelectionCase(text: String): String {
        if (text.none { it.isLetter() }) return text
        val lower = text.lowercase()
        val upper = text.uppercase()
        val title = buildString(text.length) {
            var capNext = true
            for (ch in lower) {
                if (ch.isLetter()) {
                    append(if (capNext) ch.titlecaseChar() else ch)
                    capNext = false
                } else {
                    append(ch)
                    capNext = ch.isWhitespace() || ch == '-' || ch == '\'' || ch == '/' || ch == '('
                }
            }
        }
        return when {
            text == upper -> lower
            text == lower -> if (title != upper) title else upper
            text == title -> upper
            else -> lower
        }
    }

    /** Splits a keyboard row cleanly into left and right halves for ergonomic Split Mode. */
    fun splitRowForErgonomics(row: List<String>, rowIndex: Int): Pair<List<String>, List<String>> {
        if (row.size <= 1) return row to emptyList()
        val spaceIdx = row.indexOf("SPACE")
        if (spaceIdx >= 0 && rowIndex >= 3) {
            val left = row.subList(0, spaceIdx + 1)
            val right = listOf("SPACE") + row.subList(spaceIdx + 1, row.size)
            return left to right
        }
        val mid = (row.size + 1) / 2
        return row.subList(0, mid) to row.subList(mid, row.size)
    }

    /** Extracts active Discord-style `:shortcode` query before cursor (e.g. "hey :fir" -> "fir"). */
    fun extractActiveShortcodeQuery(textBeforeCursor: String): String? {
        if (textBeforeCursor.isEmpty()) return null
        val colonIdx = textBeforeCursor.lastIndexOf(':')
        if (colonIdx < 0) return null
        if (colonIdx > 0) {
            val prev = textBeforeCursor[colonIdx - 1]
            if (!prev.isWhitespace() && prev !in setOf('(', '[', '{', '"', '\'')) {
                return null
            }
        }
        val afterColon = textBeforeCursor.substring(colonIdx + 1)
        if (afterColon.isEmpty() || afterColon.length > 24) return null
        if (!afterColon.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '+' }) {
            return null
        }
        return afterColon.lowercase()
    }

    private val DISCORD_SHORTCODES: List<Pair<String, String>> = listOf(
        "joy" to "😂",
        "laugh" to "😂",
        "rofl" to "🤣",
        "smile" to "😊",
        "grin" to "😀",
        "heart_eyes" to "😍",
        "smiling_face_with_3_hearts" to "🥰",
        "kiss" to "😘",
        "sunglasses" to "😎",
        "cool" to "😎",
        "partying" to "🥳",
        "party" to "🥳",
        "star_struck" to "🤩",
        "thinking" to "🤔",
        "think" to "🤔",
        "sweat_smile" to "😅",
        "cry" to "😭",
        "sob" to "😭",
        "rage" to "😡",
        "angry" to "😡",
        "exploding_head" to "🤯",
        "sleeping" to "😴",
        "sleep" to "😴",
        "heart" to "❤️",
        "red_heart" to "❤️",
        "orange_heart" to "🧡",
        "yellow_heart" to "💛",
        "green_heart" to "💚",
        "blue_heart" to "💙",
        "purple_heart" to "💜",
        "broken_heart" to "💔",
        "100" to "💯",
        "hundred" to "💯",
        "fire" to "🔥",
        "lit" to "🔥",
        "sparkles" to "✨",
        "star" to "⭐",
        "thumbsup" to "👍",
        "+1" to "👍",
        "thumbsdown" to "👎",
        "-1" to "👎",
        "pray" to "🙏",
        "clap" to "👏",
        "raised_hands" to "🙌",
        "handshake" to "🤝",
        "muscle" to "💪",
        "wave" to "👋",
        "v" to "✌️",
        "peace" to "✌️",
        "crossed_fingers" to "🤞",
        "eyes" to "👀",
        "white_check_mark" to "✅",
        "check" to "✅",
        "x" to "❌",
        "warning" to "⚠️",
        "tada" to "🎉",
        "cake" to "🎂",
        "pizza" to "🍕",
        "hamburger" to "🍔",
        "fries" to "🍟",
        "poultry_leg" to "🍗",
        "fish" to "🐟",
        "banana" to "🍌",
        "rice" to "🍚",
        "beer" to "🍺",
        "coffee" to "☕",
        "car" to "🚗",
        "airplane" to "✈️",
        "house" to "🏠",
        "home" to "🏠",
        "laptop" to "💻",
        "iphone" to "📱",
        "phone" to "📱",
        "moneybag" to "💰",
        "money" to "💰",
        "musical_note" to "🎵",
        "music" to "🎵",
        "soccer" to "⚽",
        "trophy" to "🏆",
        "rocket" to "🚀",
        "zap" to "⚡",
        "bulb" to "💡",
        "lock" to "🔒",
        "key" to "🔑",
        "ci" to "🇨🇮",
        "fr" to "🇫🇷",
        "us" to "🇺🇸",
        "gb" to "🇬🇧",
        "sn" to "🇸🇳",
        "cm" to "🇨🇲",
        "ml" to "🇲🇱",
        "bf" to "🇧🇫",
    )

    fun findEmojiShortcodeMatches(query: String, limit: Int = 5): List<Pair<String, String>> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        val exact = DISCORD_SHORTCODES.filter { it.first == needle }
        val prefix = DISCORD_SHORTCODES.filter { it.first != needle && it.first.startsWith(needle) }
        val partial = DISCORD_SHORTCODES.filter {
            it.first != needle && !it.first.startsWith(needle) && it.first.contains(needle)
        }
        return (exact + prefix + partial).distinctBy { it.second }.take(limit)
    }

    fun exactEmojiShortcode(code: String): String? {
        val needle = code.trim().lowercase()
        return DISCORD_SHORTCODES.firstOrNull { it.first == needle }?.second
    }

    fun wordMoveBefore(before: String): Int {
        if (before.isEmpty()) return 0
        var idx = before.length
        while (idx > 0 && before[idx - 1].isWhitespace()) idx--
        if (idx > 0 && !before[idx - 1].isLetterOrDigit()) {
            while (idx > 0 && !before[idx - 1].isLetterOrDigit() && !before[idx - 1].isWhitespace()) idx--
        } else {
            while (idx > 0 && before[idx - 1].isLetterOrDigit()) idx--
        }
        return (before.length - idx).coerceAtLeast(1)
    }

    fun wordMoveAfter(after: String): Int {
        if (after.isEmpty()) return 0
        var idx = 0
        while (idx < after.length && after[idx].isWhitespace()) idx++
        if (idx < after.length && !after[idx].isLetterOrDigit()) {
            while (idx < after.length && !after[idx].isLetterOrDigit() && !after[idx].isWhitespace()) idx++
        } else {
            while (idx < after.length && after[idx].isLetterOrDigit()) idx++
        }
        return idx.coerceAtLeast(1)
    }

    fun phraseMoveBefore(before: String): Int {
        if (before.isEmpty()) return 0
        var idx = before.length - 1
        while (idx > 0 && (before[idx].isWhitespace() || before[idx] in charArrayOf('.', '!', '?'))) {
            idx--
        }
        while (idx > 0) {
            val ch = before[idx - 1]
            if (ch == '\n' || ch == '.' || ch == '!' || ch == '?') {
                while (idx < before.length && before[idx] == ' ') idx++
                break
            }
            idx--
        }
        return (before.length - idx).coerceAtLeast(1)
    }

    fun phraseMoveAfter(after: String): Int {
        if (after.isEmpty()) return 0
        var idx = 0
        while (idx < after.length && after[idx].isWhitespace()) idx++
        while (idx < after.length) {
            val ch = after[idx]
            idx++
            if (ch == '\n' || ch == '.' || ch == '!' || ch == '?') {
                while (idx < after.length && after[idx] == ' ') idx++
                break
            }
        }
        return idx.coerceAtLeast(1)
    }
}
