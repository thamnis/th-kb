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
        "n" to "!",
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
}
