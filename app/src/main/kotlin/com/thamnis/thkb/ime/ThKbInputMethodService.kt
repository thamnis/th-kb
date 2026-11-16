package com.thamnis.thkb.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thamnis.thkb.data.AssetRepository
import com.thamnis.thkb.settings.KbPreferences
import com.thamnis.thkb.settings.SettingsActivity
import com.thamnis.thkb.ui.KeyboardRoot

class ThKbInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var preferences: KbPreferences
    private lateinit var assets: AssetRepository
    private var audioManager: AudioManager? = null
    private var inputComposeView: ComposeView? = null

    private var returnAction by mutableStateOf(ReturnAction.DEFAULT)
    private var activeEditorAction by mutableStateOf(EditorInfo.IME_ACTION_NONE)
    private var subtypeLayoutOverride by mutableStateOf<String?>(null)
    private var inputSessionKey by mutableStateOf(0)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        preferences = KbPreferences(this)
        assets = AssetRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
    }

    override fun onCreateInputView(): View {
        window?.window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            win.setBackgroundDrawableResource(android.R.color.transparent)
            @Suppress("DEPRECATION")
            win.navigationBarColor = 0xFF1B1C1E.toInt()
            win.decorView.setViewTreeLifecycleOwner(this)
            win.decorView.setViewTreeSavedStateRegistryOwner(this)
            win.decorView.setViewTreeViewModelStoreOwner(this)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            inputComposeView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@ThKbInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@ThKbInputMethodService)
            setViewTreeViewModelStoreOwner(this@ThKbInputMethodService)

            setContent {
                val settings by preferences.settings.collectAsState()
                val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId

                KeyboardRoot(
                    inputSessionKey = inputSessionKey,
                    layoutId = effectiveLayoutId,
                    autoCapOnOpen = settings.autoCapOnOpen,
                    returnAction = returnAction,
                    assets = assets,
                    onKeyFeedback = {
                        playFeedback(
                            haptics = settings.hapticsEnabled,
                            sound = settings.soundEnabled,
                        )
                    },
                    onCommitText = { text ->
                        currentInputConnection?.commitText(text, 1)
                    },
                    onBackspace = {
                        performBackspace()
                    },
                    onReturn = {
                        performReturnAction()
                    },
                    onPaste = {
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    },
                    onMoveCursor = { deltaSteps ->
                        moveCursor(deltaSteps)
                    },
                    onOpenSettings = {
                        val intent = Intent(this@ThKbInputMethodService, SettingsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    },
                )
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            inputSessionKey++
        }
        val imeOptions = info?.imeOptions ?: 0
        returnAction = KeyboardStateLogic.resolveReturnAction(imeOptions)
        activeEditorAction = imeOptions and EditorInfo.IME_MASK_ACTION
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return
        val headroomPx = (TOP_POPUP_HEADROOM_DP * resources.displayMetrics.density).toInt()
        outInsets.contentTopInsets = headroomPx
        outInsets.visibleTopInsets = headroomPx
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
    }

    companion object {
        const val TOP_POPUP_HEADROOM_DP = 68
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        subtypeLayoutOverride = when (newSubtype?.locale) {
            "fr_FR" -> "azerty_fr"
            "en_US" -> "qwerty_en"
            else -> null
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDestroy()
    }

    private fun performBackspace() {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else if (!ic.deleteSurroundingText(1, 0)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun performReturnAction() {
        val ic = currentInputConnection ?: return
        if (returnAction != ReturnAction.DEFAULT && activeEditorAction != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(activeEditorAction)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun moveCursor(deltaSteps: Int) {
        if (deltaSteps == 0) return
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null && extracted.selectionStart >= 0) {
            val textLength = extracted.text?.length ?: 0
            val target = (extracted.selectionStart + deltaSteps).coerceIn(0, textLength)
            ic.setSelection(target, target)
            return
        }

        val keyCode = if (deltaSteps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(deltaSteps)) {
            sendDownUpKeyEvents(keyCode)
        }
    }

    private fun playFeedback(haptics: Boolean, sound: Boolean) {
        if (haptics) {
            inputComposeView?.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
            )
        }
        if (sound) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        }
    }
}
