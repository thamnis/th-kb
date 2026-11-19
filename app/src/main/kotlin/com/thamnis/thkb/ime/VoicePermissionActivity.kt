package com.thamnis.thkb.ime

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object VoicePermissionBridge {
    const val EXTRA_LAUNCH_SYSTEM_RECOGNIZER = "launch_system_recognizer"
    const val EXTRA_LANGUAGE_TAG = "language_tag"

    private val _results = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val results: SharedFlow<Boolean> = _results.asSharedFlow()

    private val _spokenResults = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val spokenResults: SharedFlow<String> = _spokenResults.asSharedFlow()

    fun hasRecordAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    fun requestPermission(context: Context) {
        val intent = Intent(context, VoicePermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        runCatching { context.startActivity(intent) }
    }

    fun canLaunchSystemVoiceRecognizer(context: Context): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return intent.resolveActivity(context.packageManager) != null
    }

    fun launchSystemVoiceRecognizer(context: Context, languageTag: String) {
        val intent = Intent(context, VoicePermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(EXTRA_LAUNCH_SYSTEM_RECOGNIZER, true)
            putExtra(EXTRA_LANGUAGE_TAG, languageTag)
        }
        runCatching { context.startActivity(intent) }
    }

    internal fun notifyResult(granted: Boolean) {
        _results.tryEmit(granted)
    }

    internal fun notifySpokenText(text: String) {
        if (text.isNotBlank()) {
            _spokenResults.tryEmit(text.trim())
        }
    }
}

class VoicePermissionActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        VoicePermissionBridge.notifyResult(granted)
        finishQuietly()
    }

    private val speechActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            if (best.isNotEmpty()) {
                VoicePermissionBridge.notifySpokenText(best)
            }
        }
        finishQuietly()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchRecognizer = intent?.getBooleanExtra(
            VoicePermissionBridge.EXTRA_LAUNCH_SYSTEM_RECOGNIZER,
            false,
        ) == true

        if (launchRecognizer) {
            val lang = intent?.getStringExtra(VoicePermissionBridge.EXTRA_LANGUAGE_TAG) ?: "en-US"
            val recognizeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    if (lang.startsWith("fr", ignoreCase = true)) "Parlez maintenant…" else "Speak now…",
                )
            }
            runCatching {
                speechActivityLauncher.launch(recognizeIntent)
            }.onFailure {
                finishQuietly()
            }
            return
        }

        if (VoicePermissionBridge.hasRecordAudioPermission(this)) {
            VoicePermissionBridge.notifyResult(true)
            finishQuietly()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun finishQuietly() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
