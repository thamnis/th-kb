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
import android.os.SystemClock
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
    val ramblerMode: Boolean = false,
    val ramblerBuffer: String = "",
)

/**
 * Cleans up continuous long-form "Rambler" dictation by stripping hesitation fillers,
 * deduplicating stutters, honoring spoken line/paragraph breaks, and fixing punctuation.
 */
object RambleTextCleanup {
    private val FILLERS = Regex("""\b(um+|uh+|er+|ah+|euh+|heu+|ben|voilà quoi)\b""", RegexOption.IGNORE_CASE)
    private val MULTI_SPACE = Regex("""[ \t]{2,}""")
    private val STUTTER = Regex("""\b([a-zA-Z\u00C0-\u017F]+)\s+\1\b""", RegexOption.IGNORE_CASE)

    fun clean(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        var text = trimmed
            .replace(Regex("""\b(new paragraph|nouveau paragraphe)\b""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""\b(new line|à la ligne|ligne suivante)\b""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""\b(period|point final|full stop)\b""", RegexOption.IGNORE_CASE), ".")
            .replace(Regex("""\b(comma|virgule)\b""", RegexOption.IGNORE_CASE), ",")
            .replace(FILLERS, "")
        text = STUTTER.replace(text, "$1")
        text = text.replace(MULTI_SPACE, " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(Regex(""" *\n *"""), "\n")
            .trim()

        // Capitalize start of each sentence or paragraph
        val out = StringBuilder(text.length + 2)
        var capNext = true
        for (ch in text) {
            if (capNext && ch.isLetter()) {
                out.append(ch.titlecaseChar())
                capNext = false
            } else {
                out.append(ch)
                if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') {
                    capNext = true
                } else if (!ch.isWhitespace()) {
                    capNext = false
                }
            }
        }
        val finalStr = out.toString().trim()
        return if (finalStr.isNotEmpty() && finalStr.last().isLetterOrDigit()) {
            "$finalStr."
        } else {
            finalStr
        }
    }
}

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
    private val onPartialText: (String) -> Unit = {},
    private val onCommittedText: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var sessionActive = false
    private var userRequestedStop = false
    private var triedDefaultFallback = false
    private var lastVoiceActivityMs = 0L
    private var accumulatedSessionWords = ""

    companion object {
        private const val VOICE_MAX_SILENCE_WAIT_MS = 14_000L
        private const val RAMBLER_MAX_SILENCE_WAIT_MS = 60_000L
        private const val COMPLETE_SILENCE_EXTRA_MS = 7_000L
        private const val POSSIBLY_COMPLETE_SILENCE_EXTRA_MS = 5_000L
        private const val MIN_SPEECH_LENGTH_EXTRA_MS = 12_000L
    }

    fun setRamblerMode(enabled: Boolean) {
        _state.value = _state.value.copy(
            ramblerMode = enabled,
            ramblerBuffer = if (!enabled) "" else _state.value.ramblerBuffer,
        )
    }

    fun consumeRamblerBuffer(cleaned: Boolean = true): String {
        val combined = listOf(_state.value.ramblerBuffer, _state.value.partialText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        val result = if (cleaned) RambleTextCleanup.clean(combined) else combined
        accumulatedSessionWords = ""
        _state.value = _state.value.copy(ramblerBuffer = "", partialText = "")
        return result
    }

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
            mainHandler.removeCallbacksAndMessages(null)
            userRequestedStop = false
            lastVoiceActivityMs = SystemClock.uptimeMillis()
            accumulatedSessionWords = ""
            startListeningOnMain(languageTag, useUnboundDefault, isContinuation = false)
        }
    }

    private fun startListeningOnMain(
        languageTag: String,
        useUnboundDefault: Boolean,
        isContinuation: Boolean = false,
    ) {
        if (!VoicePermissionBridge.hasRecordAudioPermission(appContext)) {
            _state.value = _state.value.copy(
                panelVisible = true,
                isListening = false,
                languageTag = languageTag,
                errorMessage = "Tap Mic to grant microphone access.",
            )
            return
        }

        val carryOverText = if (isContinuation) accumulatedSessionWords else ""
        stopInternalOnMain(keepPanelOpen = true, resetError = true)
        accumulatedSessionWords = carryOverText
        if (!useUnboundDefault && !isContinuation) {
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
            // Keep voice typing strictly inside th-kb (never launch external Google Voice dialog)
            startDirectPcmMonitor(languageTag)
            return
        }

        recognizer = sr
        sessionActive = true
        _state.value = VoiceUiState(
            panelVisible = true,
            isListening = true,
            isOnDevice = true,
            languageTag = languageTag,
            partialText = carryOverText,
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

            override fun onBeginningOfSpeech() {
                lastVoiceActivityMs = SystemClock.uptimeMillis()
            }

            override fun onRmsChanged(rmsdB: Float) {
                val clamped = rmsdB.coerceIn(0f, 10f)
                if (clamped >= 3.2f) {
                    lastVoiceActivityMs = SystemClock.uptimeMillis()
                }
                _state.value = _state.value.copy(rmsDb = clamped)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                if (!sessionActive) return
                sessionActive = false

                val isServiceBindError = error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == 12 || error == 13

                if (isServiceBindError && !triedDefaultFallback && preferredComponent != null) {
                    triedDefaultFallback = true
                    mainHandler.postDelayed({
                        if (!userRequestedStop) {
                            startListeningOnMain(languageTag, useUnboundDefault = true, isContinuation = isContinuation)
                        }
                    }, 240L)
                    return
                }

                // Recover in-place inside th-kb without ever launching external Google Voice Typing popup
                val isRecoverableInKb = error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                val elapsedSilence = SystemClock.uptimeMillis() - lastVoiceActivityMs
                if (isRecoverableInKb && !userRequestedStop && elapsedSilence < VOICE_MAX_SILENCE_WAIT_MS) {
                    mainHandler.removeCallbacksAndMessages(null)
                    mainHandler.postDelayed({
                        if (!userRequestedStop) {
                            startListeningOnMain(languageTag, useUnboundDefault, isContinuation = true)
                        }
                    }, 280L)
                    return
                }

                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone busy. Tap Mic to retry."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    -> "Offline voice pack needed for ${languageTag.uppercase()}."
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> "Listening paused after silence. Tap Mic to speak."
                    else -> "Tap Mic to continue speaking."
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
                val currentSegmentPartial = _state.value.partialText
                    .removePrefix(accumulatedSessionWords)
                    .trim()
                val finalSpoken = best.ifEmpty { currentSegmentPartial }
                val isRambler = _state.value.ramblerMode
                if (finalSpoken.isNotEmpty()) {
                    lastVoiceActivityMs = SystemClock.uptimeMillis()
                    accumulatedSessionWords = if (accumulatedSessionWords.isEmpty()) {
                        finalSpoken
                    } else {
                        "$accumulatedSessionWords $finalSpoken"
                    }
                    if (isRambler) {
                        val nextBuf = if (_state.value.ramblerBuffer.isEmpty()) {
                            finalSpoken
                        } else {
                            "${_state.value.ramblerBuffer} $finalSpoken"
                        }
                        _state.value = _state.value.copy(ramblerBuffer = nextBuf, partialText = "")
                    } else {
                        onCommittedText(finalSpoken)
                    }
                }

                val elapsedSilence = SystemClock.uptimeMillis() - lastVoiceActivityMs
                val maxWait = if (isRambler) RAMBLER_MAX_SILENCE_WAIT_MS else VOICE_MAX_SILENCE_WAIT_MS
                if (!userRequestedStop && elapsedSilence < maxWait) {
                    mainHandler.removeCallbacksAndMessages(null)
                    mainHandler.postDelayed({
                        if (!userRequestedStop) {
                            startListeningOnMain(languageTag, useUnboundDefault, isContinuation = true)
                        }
                    }, 260L)
                } else if (!isRambler) {
                    _state.value = _state.value.copy(
                        panelVisible = false,
                        isListening = false,
                        partialText = "",
                        rmsDb = 0f,
                        errorMessage = null,
                    )
                } else {
                    _state.value = _state.value.copy(
                        isListening = false,
                        rmsDb = 0f,
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    lastVoiceActivityMs = SystemClock.uptimeMillis()
                    val combined = if (accumulatedSessionWords.isEmpty()) {
                        partial
                    } else {
                        "$accumulatedSessionWords $partial"
                    }
                    _state.value = _state.value.copy(partialText = combined)
                    if (!_state.value.ramblerMode) {
                        onPartialText(partial)
                    }
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
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, COMPLETE_SILENCE_EXTRA_MS)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                POSSIBLY_COMPLETE_SILENCE_EXTRA_MS,
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MIN_SPEECH_LENGTH_EXTRA_MS)
        }
        runCatching {
            sr.startListening(intent)
        }.onFailure {
            _state.value = _state.value.copy(
                panelVisible = true,
                isListening = false,
                errorMessage = "Tap Mic to retry voice typing.",
            )
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
            userRequestedStop = true
            val currentSegmentPartial = _state.value.partialText
                .removePrefix(accumulatedSessionWords)
                .trim()
            val wasListening = sessionActive
            val sr = recognizer
            if (wasListening && sr != null) {
                runCatching { sr.stopListening() }
                return@post
            }
            stopInternalOnMain(keepPanelOpen = false, resetError = true)
            accumulatedSessionWords = ""
            if (currentSegmentPartial.isNotEmpty()) {
                onCommittedText(currentSegmentPartial)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            userRequestedStop = true
            accumulatedSessionWords = ""
            stopInternalOnMain(keepPanelOpen = false, resetError = true)
        }
    }

    fun destroy() {
        mainHandler.post {
            userRequestedStop = true
            accumulatedSessionWords = ""
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
