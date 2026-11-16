package com.thamnis.thkb.ime

import android.view.inputmethod.EditorInfo

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK,
}

enum class KeyboardLayer {
    ALPHA,
    SYMBOLS,
    SYMBOLS_MORE,
}

enum class PanelMode {
    KEYBOARD,
    EMOJI,
    KAOMOJI,
}

enum class ReturnAction(val label: String) {
    DEFAULT("↵"),
    GO("Go"),
    SEARCH("Search"),
    SEND("Send"),
    NEXT("Next"),
    DONE("Done"),
}

data class AccentPopupState(
    val baseKey: String,
    val variants: List<String>,
    val selectedIndex: Int = 0,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
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
}
