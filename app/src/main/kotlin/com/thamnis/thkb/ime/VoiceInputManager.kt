package com.thamnis.thkb.ime

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceUiState(
    val panelVisible: Boolean = false,
    val isListening: Boolean = false,
    val isOnDevice: Boolean = false,
    val languageTag: String = "en-US",
    val partialText: String = "",
    val rmsDb: Float = 0f,
    val errorMessage: String? = null,
)

/**
 * Manages speech recognition for th-kb on the Main Looper.
 * Uses the system default SpeechRecognizer service first, falls back to Android 12+
 * on-device recognizer or the system RecognizerIntent dialog if direct service binding fails.
 */
class VoiceInputManager(
    context: Context,
    private val onCommittedText: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var sessionActive = false
    private var triedAlternateEngine = false

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun isDirectServiceAvailable(): Boolean {
        if (SpeechRecognizer.isRecognitionAvailable(appContext)) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
    }

    fun startListening(languageTag: String, preferOnDeviceAttempt: Boolean = false) {
        mainHandler.post {
            startListeningOnMain(languageTag, preferOnDeviceAttempt)
        }
    }

    private fun startListeningOnMain(languageTag: String, preferOnDeviceAttempt: Boolean) {
        if (!VoicePermissionBridge.hasRecordAudioPermission(appContext)) {
            _state.value = _state.value.copy(
                panelVisible = true,
                isListening = false,
                languageTag = languageTag,
                errorMessage = "Tap Mic to grant microphone access.",
            )
            return
        }

        if (!isDirectServiceAvailable()) {
            if (VoicePermissionBridge.canLaunchSystemVoiceRecognizer(appContext)) {
                _state.value = _state.value.copy(
                    panelVisible = false,
                    isListening = false,
                    languageTag = languageTag,
                    errorMessage = null,
                )
                VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
            } else {
                _state.value = _state.value.copy(
                    panelVisible = true,
                    isListening = false,
                    languageTag = languageTag,
                    errorMessage = "No speech recognition service installed on this phone.",
                )
            }
            return
        }

        stopInternalOnMain(keepPanelOpen = true, resetError = true)
        if (!preferOnDeviceAttempt) {
            triedAlternateEngine = false
        }

        val canUseOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        val useOnDevice = preferOnDeviceAttempt && canUseOnDevice

        val sr = runCatching {
            if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
                SpeechRecognizer.createSpeechRecognizer(appContext)
            } else if (canUseOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else {
                null
            }
        }.getOrNull()

        if (sr == null) {
            if (VoicePermissionBridge.canLaunchSystemVoiceRecognizer(appContext)) {
                VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
            } else {
                _state.value = _state.value.copy(
                    panelVisible = true,
                    isListening = false,
                    languageTag = languageTag,
                    errorMessage = "Could not bind speech recognizer.",
                )
            }
            return
        }

        recognizer = sr
        sessionActive = true
        _state.value = VoiceUiState(
            panelVisible = true,
            isListening = true,
            isOnDevice = useOnDevice,
            languageTag = languageTag,
            partialText = "",
            rmsDb = 0f,
            errorMessage = null,
        )

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = _state.value.copy(
                    panelVisible = true,
                    isListening = true,
                    errorMessage = null,
                )
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                val clamped = rmsdB.coerceIn(0f, 10f)
                _state.value = _state.value.copy(rmsDb = clamped)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                if (!sessionActive) return
                sessionActive = false

                // If primary engine failed due to client/service/language pack binding, try alternate or system dialog
                val isBindingOrPackError = error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                    error == 12 /* ERROR_LANGUAGE_NOT_SUPPORTED */ ||
                    error == 13 /* ERROR_LANGUAGE_UNAVAILABLE */ ||
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

                if (isBindingOrPackError && !triedAlternateEngine && canUseOnDevice && !useOnDevice) {
                    triedAlternateEngine = true
                    startListeningOnMain(languageTag, preferOnDeviceAttempt = true)
                    return
                }

                if (isBindingOrPackError && VoicePermissionBridge.canLaunchSystemVoiceRecognizer(appContext)) {
                    stopInternalOnMain(keepPanelOpen = false, resetError = true)
                    VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
                    return
                }

                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Tap Mic to retry."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    -> "Offline speech pack not installed. Tap System Voice or install offline pack."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap Mic to try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap Mic to speak."
                    else -> "Voice recognition unavailable ($error). Tap System Voice below."
                }
                _state.value = _state.value.copy(
                    panelVisible = true,
                    isListening = false,
                    rmsDb = 0f,
                    errorMessage = msg,
                )
            }

            override fun onResults(results: Bundle?) {
                sessionActive = false
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                val fallbackPartial = _state.value.partialText.trim()
                val finalSpoken = best.ifEmpty { fallbackPartial }
                _state.value = _state.value.copy(
                    panelVisible = false,
                    isListening = false,
                    partialText = "",
                    rmsDb = 0f,
                    errorMessage = null,
                )
                if (finalSpoken.isNotEmpty()) {
                    onCommittedText(finalSpoken)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    _state.value = _state.value.copy(partialText = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            if (useOnDevice) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
        runCatching {
            sr.startListening(intent)
        }.onFailure {
            if (VoicePermissionBridge.canLaunchSystemVoiceRecognizer(appContext)) {
                stopInternalOnMain(keepPanelOpen = false, resetError = true)
                VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
            } else {
                _state.value = _state.value.copy(
                    panelVisible = true,
                    isListening = false,
                    errorMessage = "Unable to start microphone recognizer.",
                )
            }
        }
    }

    fun launchSystemPopupFallback(languageTag: String) {
        mainHandler.post {
            stopInternalOnMain(keepPanelOpen = false, resetError = true)
            VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
        }
    }

    fun stopAndCommitIfPartial() {
        mainHandler.post {
            val currentPartial = _state.value.partialText.trim()
            val wasListening = sessionActive
            val sr = recognizer
            if (wasListening && sr != null) {
                runCatching { sr.stopListening() }
                return@post
            }
            stopInternalOnMain(keepPanelOpen = false, resetError = true)
            if (currentPartial.isNotEmpty()) {
                onCommittedText(currentPartial)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            stopInternalOnMain(keepPanelOpen = false, resetError = true)
        }
    }

    fun destroy() {
        mainHandler.post {
            stopInternalOnMain(keepPanelOpen = false, resetError = true)
        }
    }

    private fun stopInternalOnMain(keepPanelOpen: Boolean, resetError: Boolean) {
        sessionActive = false
        recognizer?.run {
            runCatching { cancel() }
            runCatching { destroy() }
        }
        recognizer = null
        _state.value = _state.value.copy(
            panelVisible = keepPanelOpen,
            isListening = false,
            partialText = "",
            rmsDb = 0f,
            errorMessage = if (resetError) null else _state.value.errorMessage,
        )
    }
}
