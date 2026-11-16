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
    fun nextShiftOnTap(current: ShiftState): ShiftState = when (current) {
        ShiftState.OFF -> ShiftState.ONCE
        ShiftState.ONCE -> ShiftState.OFF
        ShiftState.CAPS_LOCK -> ShiftState.OFF
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
            "SPACE" -> "th-kb"
            "RETURN" -> returnAction.label
            else -> if (shiftState != ShiftState.OFF && rawKey.length == 1 && rawKey[0].isLetter()) {
                rawKey.uppercase()
            } else {
                rawKey
            }
        }

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
