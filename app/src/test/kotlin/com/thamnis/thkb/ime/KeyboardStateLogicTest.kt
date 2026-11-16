package com.thamnis.thkb.ime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardStateLogicTest {

    @Test
    fun shiftTransitionsFollowThreeStateModel() {
        assertEquals(ShiftState.ONCE, KeyboardStateLogic.nextShiftOnTap(ShiftState.OFF))
        assertEquals(ShiftState.OFF, KeyboardStateLogic.nextShiftOnTap(ShiftState.ONCE))
        assertEquals(ShiftState.OFF, KeyboardStateLogic.nextShiftOnTap(ShiftState.CAPS_LOCK))

        assertEquals(ShiftState.CAPS_LOCK, KeyboardStateLogic.nextShiftOnLongPress(ShiftState.OFF))
        assertEquals(ShiftState.CAPS_LOCK, KeyboardStateLogic.nextShiftOnLongPress(ShiftState.ONCE))
        assertEquals(ShiftState.OFF, KeyboardStateLogic.nextShiftOnLongPress(ShiftState.CAPS_LOCK))

        assertEquals(ShiftState.OFF, KeyboardStateLogic.shiftAfterCharacterCommit(ShiftState.ONCE))
        assertEquals(ShiftState.CAPS_LOCK, KeyboardStateLogic.shiftAfterCharacterCommit(ShiftState.CAPS_LOCK))
    }

    @Test
    fun resolveKeyLabelRespectsShiftAndReturnAction() {
        assertEquals("a", KeyboardStateLogic.resolveKeyLabel("a", ShiftState.OFF, ReturnAction.DEFAULT))
        assertEquals("A", KeyboardStateLogic.resolveKeyLabel("a", ShiftState.ONCE, ReturnAction.DEFAULT))
        assertEquals("Search", KeyboardStateLogic.resolveKeyLabel("RETURN", ShiftState.OFF, ReturnAction.SEARCH))
        assertEquals("?123", KeyboardStateLogic.resolveKeyLabel("SYMBOLS", ShiftState.OFF, ReturnAction.DEFAULT))
        assertEquals("=\\<", KeyboardStateLogic.resolveKeyLabel("MORE_SYMBOLS", ShiftState.OFF, ReturnAction.DEFAULT))
    }

    @Test
    fun resolveReturnActionMapsImeOptions() {
        assertEquals(ReturnAction.SEARCH, KeyboardStateLogic.resolveReturnAction(EditorInfo.IME_ACTION_SEARCH))
        assertEquals(ReturnAction.SEND, KeyboardStateLogic.resolveReturnAction(EditorInfo.IME_ACTION_SEND))
        assertEquals(ReturnAction.DONE, KeyboardStateLogic.resolveReturnAction(EditorInfo.IME_ACTION_DONE))
        assertEquals(
            ReturnAction.DEFAULT,
            KeyboardStateLogic.resolveReturnAction(EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_ENTER_ACTION),
        )
    }
}
