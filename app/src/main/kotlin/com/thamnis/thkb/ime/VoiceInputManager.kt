package com.thamnis.thkb.ime

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.thamnis.thkb.engine.SuggestionEngine
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
 *
 * Automatically discovers the best accessible RecognitionService component on the device
 * (preferring FUTO Voice Input, Sherpa/Whisper, or GoogleTTSRecognitionService with
 * offline language packs over restricted private AiAi system services), and provides a
 * Rust-backed AudioRecord PCM VAD & RMS analyzer (`SuggestionEngine.analyzePcmRms`).
 */
class VoiceInputManager(
    context: Context,
    private val onCommittedText: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var sessionActive = false
    private var triedDefaultFallback = false

    @Volatile
    private var pcmRecordActive = false
    private var pcmThread: Thread? = null

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    /**
     * Finds the best accessible RecognitionService component on the device, avoiding
     * private system-only services (`com.google.android.as`) that reject 3rd-party keyboard UIDs.
     */
    private fun resolvePreferredRecognitionComponent(): ComponentName? {
        return runCatching {
            val services = appContext.packageManager.queryIntentServices(
                Intent(RecognitionService.SERVICE_INTERFACE),
                0,
            )
            val candidates = services.mapNotNull { it.serviceInfo }
                .filter { it.exported && (it.permission == null || it.packageName == "com.google.android.tts") }

            // 1. Prefer dedicated offline open-source voice engines if installed (FUTO, Sherpa, Whisper, Kõnele)
            val openOffline = candidates.firstOrNull { info ->
                val pkg = info.packageName.lowercase()
                pkg.contains("futo") || pkg.contains("whisper") || pkg.contains("sherpa") ||
                    pkg.contains("vosk") || pkg.contains("konele")
            }
            if (openOffline != null) {
                return ComponentName(openOffline.packageName, openOffline.name)
            }

            // 2. Prefer GoogleTTSRecognitionService (`com.google.android.tts`) which supports 3rd-party IMEs + offline packs
            val googleTts = candidates.firstOrNull { info ->
                info.packageName == "com.google.android.tts"
            }
            if (googleTts != null) {
                return ComponentName(googleTts.packageName, googleTts.name)
            }

            // 3. Any other non-AiAi exported recognition service
            val nonAiAi = candidates.firstOrNull { info ->
                info.packageName != "com.google.android.as"
            }
            nonAiAi?.let { ComponentName(it.packageName, it.name) }
        }.getOrNull()
    }

    fun startListening(languageTag: String, useUnboundDefault: Boolean = false) {
        mainHandler.post {
            startListeningOnMain(languageTag, useUnboundDefault)
        }
    }

    private fun startListeningOnMain(languageTag: String, useUnboundDefault: Boolean) {
        if (!VoicePermissionBridge.hasRecordAudioPermission(appContext)) {
            _state.value = _state.value.copy(
                panelVisible = true,
                isListening = false,
                languageTag = languageTag,
                errorMessage = "Tap Mic to grant microphone access.",
            )
            return
        }

        stopInternalOnMain(keepPanelOpen = true, resetError = true)
        if (!useUnboundDefault) {
            triedDefaultFallback = false
        }

        val preferredComponent = if (!useUnboundDefault) resolvePreferredRecognitionComponent() else null

        val sr = runCatching {
            when {
                preferredComponent != null ->
                    SpeechRecognizer.createSpeechRecognizer(appContext, preferredComponent)
                SpeechRecognizer.isRecognitionAvailable(appContext) ->
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext) ->
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                else -> null
            }
        }.getOrNull()

        if (sr == null) {
            // Start Rust-backed direct PCM microphone monitor + launch system voice dialog if available
            startDirectPcmMonitor(languageTag)
            if (VoicePermissionBridge.canLaunchSystemVoiceRecognizer(appContext)) {
                VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
            }
            return
        }

        recognizer = sr
        sessionActive = true
        _state.value = VoiceUiState(
            panelVisible = true,
            isListening = true,
            isOnDevice = true,
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

                val isServiceBindError = error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == 12 || error == 13

                if (isServiceBindError && !triedDefaultFallback && preferredComponent != null) {
                    triedDefaultFallback = true
                    startListeningOnMain(languageTag, useUnboundDefault = true)
                    return
                }

                if (isServiceBindError && VoicePermissionBridge.canLaunchSystemVoiceRecognizer(appContext)) {
                    stopInternalOnMain(keepPanelOpen = false, resetError = true)
                    VoicePermissionBridge.launchSystemVoiceRecognizer(appContext, languageTag)
                    return
                }

                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone busy. Tap Mic to retry."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    -> "Offline voice pack needed. Tap System Voice ↗ or download pack."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap Mic to speak again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap Mic to speak."
                    else -> "Voice service busy ($error). Tap System Voice ↗."
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
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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
                    errorMessage = "Could not start speech recognizer.",
                )
            }
        }
    }

    /**
     * Direct 16kHz PCM AudioRecord monitor analyzed via native Rust `analyze_pcm16_rms`
     * when no external RecognitionService is bound.
     */
    @SuppressLint("MissingPermission")
    private fun startDirectPcmMonitor(languageTag: String) {
        if (!VoicePermissionBridge.hasRecordAudioPermission(appContext)) return
        stopPcmMonitor()
        pcmRecordActive = true
        _state.value = VoiceUiState(
            panelVisible = true,
            isListening = true,
            isOnDevice = true,
            languageTag = languageTag,
            partialText = "",
            rmsDb = 0f,
            errorMessage = null,
        )
        pcmThread = Thread {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(3200)
            val record = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf,
                )
            }.getOrNull() ?: return@Thread

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { record.release() }
                return@Thread
            }
            val frame = ShortArray(1024)
            runCatching {
                record.startRecording()
                while (pcmRecordActive) {
                    val read = record.read(frame, 0, frame.size)
                    if (read > 0) {
                        val rms = SuggestionEngine.analyzePcmRms(frame, read)
                        mainHandler.post {
                            if (pcmRecordActive) {
                                _state.value = _state.value.copy(rmsDb = rms)
                            }
                        }
                    }
                }
            }
            runCatching {
                record.stop()
                record.release()
            }
        }.apply { start() }
    }

    private fun stopPcmMonitor() {
        pcmRecordActive = false
        pcmThread = null
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
        stopPcmMonitor()
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
