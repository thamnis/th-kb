package com.thamnis.thkb.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.thamnis.thkb.data.ClipboardHistoryManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

object OcrScanBridge {
    private val _results = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val results = _results.asSharedFlow()

    fun deliverScannedText(text: String) {
        val clean = text.trim()
        if (clean.isNotEmpty()) {
            _results.tryEmit(clean)
        }
    }
}

class OcrScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val latestClipUri = ClipboardHistoryManager(this).entries.value
            .firstOrNull { it.isRichMedia }
            ?.mediaUri
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2563EB),
                    background = Color(0xFFF5F6FA),
                    surface = Color(0xFFFFFFFF),
                ),
            ) {
                var recognizedText by remember { mutableStateOf("") }
                var statusMsg by remember {
                    mutableStateOf("Capture a photo, choose an image, or scan your latest clipboard screenshot.")
                }
                var isProcessing by remember { mutableStateOf(false) }

                val cameraOutUri = remember {
                    val dir = File(filesDir, "ocr_cache").apply { mkdirs() }
                    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                }

                fun runOcr(uri: Uri) {
                    isProcessing = true
                    statusMsg = "Recognizing text on device..."
                    runCatching {
                        val image = InputImage.fromFilePath(this, uri)
                        recognizer.process(image)
                            .addOnSuccessListener { result ->
                                recognizedText = result.text.trim()
                                statusMsg = if (recognizedText.isEmpty()) {
                                    "No readable text detected in this image."
                                } else {
                                    "Detected ${recognizedText.lineSequence().count()} line(s). Edit if needed, then Insert."
                                }
                                isProcessing = false
                            }
                            .addOnFailureListener { err ->
                                statusMsg = "OCR failed: ${err.message}"
                                isProcessing = false
                            }
                    }.onFailure { err ->
                        statusMsg = "Could not load image: ${err.message}"
                        isProcessing = false
                    }
                }

                val pickImageLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent(),
                ) { uri ->
                    if (uri != null) runOcr(uri)
                }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture(),
                ) { saved ->
                    if (saved) runOcr(cameraOutUri)
                }

                val autoScanUri = remember {
                    intent.getStringExtra("initial_uri")?.let { runCatching { Uri.parse(it) }.getOrNull() }
                }
                LaunchedEffect(autoScanUri) {
                    if (autoScanUri != null) {
                        runOcr(autoScanUri)
                    }
                }

                Scaffold(
                    containerColor = Color(0xFFF5F6FA),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Back",
                                )
                            }
                            Text(
                                text = "OCR Text Scanner",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF181A1F),
                            )
                        }
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch(cameraOutUri) },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Camera")
                            }
                            OutlinedButton(
                                onClick = { pickImageLauncher.launch("image/*") },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Gallery")
                            }
                        }

                        if (latestClipUri != null) {
                            OutlinedButton(
                                onClick = { runOcr(latestClipUri) },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Scan Latest Clipboard Screenshot / Image")
                            }
                        }

                        Text(
                            text = statusMsg,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                        )

                        OutlinedTextField(
                            value = recognizedText,
                            onValueChange = { recognizedText = it },
                            label = { Text("Recognized Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("OCR Text", recognizedText))
                                    statusMsg = "Copied to clipboard."
                                },
                                enabled = recognizedText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Copy")
                            }
                            Button(
                                onClick = {
                                    OcrScanBridge.deliverScannedText(recognizedText)
                                    finish()
                                },
                                enabled = recognizedText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1.3f),
                            ) {
                                Text("Insert into Text Field")
                            }
                        }
                    }
                }
            }
        }
    }
}
