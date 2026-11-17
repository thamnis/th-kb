package com.thamnis.thkb.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardStateLogicTest {

    @Test
    fun shiftTransitionsFollowThreeStateModel() {
        assertEquals(ShiftState.ONCE, KeyboardStateLogic.nextShiftOnTap(ShiftState.OFF))
        assertEquals(ShiftState.OFF, KeyboardStateLogic.nextShiftOnTap(ShiftState.ONCE))
        assertEquals(ShiftState.CAPS_LOCK, KeyboardStateLogic.nextShiftOnTap(ShiftState.ONCE, isDoubleTap = true))
        assertEquals(ShiftState.OFF, KeyboardStateLogic.nextShiftOnTap(ShiftState.CAPS_LOCK))

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

    @Test
    fun resolveInputContextDetectsTerminalUrlEmailNumberAndPassword() {
        assertEquals(
            InputContext.TERMINAL,
            KeyboardStateLogic.resolveInputContext(InputType.TYPE_CLASS_TEXT, "com.termux"),
        )
        assertEquals(
            InputContext.URL,
            KeyboardStateLogic.resolveInputContext(
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
                "com.android.chrome",
            ),
        )
        assertEquals(
            InputContext.EMAIL,
            KeyboardStateLogic.resolveInputContext(
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                "com.google.android.gm",
            ),
        )
        assertEquals(
            InputContext.PASSWORD,
            KeyboardStateLogic.resolveInputContext(
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                "com.example.app",
            ),
        )
        assertEquals(
            InputContext.NUMBER,
            KeyboardStateLogic.resolveInputContext(InputType.TYPE_CLASS_NUMBER, "com.example.app"),
        )
        assertTrue(KeyboardStateLogic.shouldAutoCapOnOpen(InputContext.NORMAL, true))
        assertFalse(KeyboardStateLogic.shouldAutoCapOnOpen(InputContext.PASSWORD, true))
        assertFalse(KeyboardStateLogic.shouldAutoCapOnOpen(InputContext.URL, true))
        assertEquals(KeyboardLayer.NUMERIC, KeyboardStateLogic.initialLayerForContext(InputContext.NUMBER))
        assertEquals(KeyboardLayer.ALPHA, KeyboardStateLogic.initialLayerForContext(InputContext.NORMAL))
    }

    @Test
    fun wordAndParagraphDeleteCountsComputeExpectedRanges() {
        assertEquals(5, KeyboardStateLogic.wordDeleteCountBefore("hello world"))
        assertEquals(6, KeyboardStateLogic.wordDeleteCountBefore("hello world "))
        assertEquals(5, KeyboardStateLogic.wordDeleteCountAfter("world next"))
        assertEquals(6, KeyboardStateLogic.wordDeleteCountAfter(" world next"))

        assertEquals(11, KeyboardStateLogic.paragraphDeleteCountBefore("first line\nsecond line"))
        assertEquals(11, KeyboardStateLogic.paragraphDeleteCountBefore("single line"))
        assertEquals(11, KeyboardStateLogic.paragraphDeleteCountAfter("first line\nsecond line"))
    }

    @Test
    fun shouldAutoCapAfterSentencePunctuationAndNewline() {
        assertTrue(KeyboardStateLogic.shouldAutoCapBeforeCursor("", InputContext.NORMAL, true))
        assertTrue(KeyboardStateLogic.shouldAutoCapBeforeCursor("Hello.\n", InputContext.NORMAL, true))
        assertTrue(KeyboardStateLogic.shouldAutoCapBeforeCursor("Hello. ", InputContext.NORMAL, true))
        assertTrue(KeyboardStateLogic.shouldAutoCapBeforeCursor("What?! ", InputContext.NORMAL, true))
        assertFalse(KeyboardStateLogic.shouldAutoCapBeforeCursor("Hello ", InputContext.NORMAL, true))
        assertFalse(KeyboardStateLogic.shouldAutoCapBeforeCursor("Hello.", InputContext.NORMAL, true))
        assertFalse(KeyboardStateLogic.shouldAutoCapBeforeCursor("Hello. ", InputContext.URL, true))
        assertFalse(KeyboardStateLogic.shouldAutoCapBeforeCursor("Hello. ", InputContext.NORMAL, false))
    }

    @Test
    fun backupCodecRoundTripsSettingsAndPinnedClips() {
        val original = com.thamnis.thkb.settings.KbSettings(
            layoutId = "azerty_fr",
            hapticsEnabled = false,
            soundEnabled = true,
            autoCapOnOpen = false,
            showKeyPreview = false,
            longPressDelayMs = 600L,
            rapidDeleteDelayMs = 750L,
            deleteGesturesEnabled = false,
            resetForwardDeleteOnChar = true,
            emojiGridColumns = 8,
            favoriteItems = setOf("🔥", "(^_^)"),
            recentEmojis = listOf("🚀", "🔥"),
        )
        val clips = listOf(
            com.thamnis.thkb.data.ClipboardEntry("pinned clip", pinned = true),
            com.thamnis.thkb.data.ClipboardEntry("temp clip", pinned = false),
        )
        val json = com.thamnis.thkb.settings.KbBackupCodec.encode(original, clips)
        val (decoded, decodedClips) = com.thamnis.thkb.settings.KbBackupCodec.decode(json)
        assertEquals(original, decoded)
        assertEquals(1, decodedClips.size)
        assertEquals("pinned clip", decodedClips[0].text)
    }

    @Test
    fun backupCodecRoundTripsTodoTasks() {
        val original = com.thamnis.thkb.settings.KbSettings()
        val tasks = listOf(
            com.thamnis.thkb.data.TodoTask(id = "1", title = "Buy groceries", isCompleted = false),
            com.thamnis.thkb.data.TodoTask(id = "2", title = "Ship v1.0", isCompleted = true),
        )
        val json = com.thamnis.thkb.settings.KbBackupCodec.encode(original, todoTasks = tasks)
        val decodedFull = com.thamnis.thkb.settings.KbBackupCodec.decodeFull(json)
        assertEquals(2, decodedFull.todoTasks.size)
        assertEquals("Buy groceries", decodedFull.todoTasks[0].title)
        assertFalse(decodedFull.todoTasks[0].isCompleted)
        assertEquals("Ship v1.0", decodedFull.todoTasks[1].title)
        assertTrue(decodedFull.todoTasks[1].isCompleted)
    }
}
