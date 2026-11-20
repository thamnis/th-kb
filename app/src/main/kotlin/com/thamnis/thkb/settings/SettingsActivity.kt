package com.thamnis.thkb.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.BuildConfig
import com.thamnis.thkb.data.ClipboardHistoryManager
import com.thamnis.thkb.data.DialectPack
import com.thamnis.thkb.data.DialectPackManager
import com.thamnis.thkb.data.PersonalDictionaryManager
import com.thamnis.thkb.data.TodoListManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SettingsCanvasBg = Color(0xFFF5F6FA)
private val SettingsCardBg = Color(0xFFFFFFFF)
private val SettingsBorderColor = Color(0xFFECEEF3)
private val SettingsDividerColor = Color(0xFFF1F3F7)
private val SettingsTextPrimary = Color(0xFF181A1F)
private val SettingsTextSecondary = Color(0xFF6B7280)
private val SettingsIconTint = Color(0xFF4B5563)
private val SettingsSwitchGreen = Color(0xFF22C55E)
private val SettingsAccentBlue = Color(0xFF2563EB)
private val SettingsPillSoftBlue = Color(0xFFEFF6FF)

private data class LicenseArticle(
    val title: String,
    val holder: String,
    val summary: String,
    val body: String,
)

private enum class SettingsPage(val title: String) {
    MAIN("Settings"),
    TRY_KEYBOARD("Try Keyboard"),
    LAYOUT("Language & Layout"),
    DIALECT_STORE("Dialect & Jargon Store"),
    DIALECT_DETAIL("Dialect Vocabulary"),
    PERSONAL_DICT("My Custom Words"),
    BACKUP("Backup & Restore"),
    ABOUT_LICENSES("Author Rights & Licenses"),
    LICENSE_DETAIL("License & Terms"),
}

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = KbPreferences(this)
        val clipboardHistory = ClipboardHistoryManager(this)
        val todoManager = TodoListManager(this)
        val personalDictionary = PersonalDictionaryManager(this)
        val dialectManager = DialectPackManager(this)

        setContent {
            val settings by preferences.settings.collectAsState()
            val clips by clipboardHistory.entries.collectAsState()
            val tasks by todoManager.tasks.collectAsState()
            val personalState by personalDictionary.state.collectAsState()
            val dialectState by dialectManager.state.collectAsState()

            var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
            var selectedLicense by remember { mutableStateOf<LicenseArticle?>(null) }
            var selectedDialectPackId by remember { mutableStateOf<String?>(null) }
            var dialectWordSearch by remember { mutableStateOf("") }
            var testText by remember { mutableStateOf("") }
            var newCustomWord by remember { mutableStateOf("") }
            var backupStatus by remember { mutableStateOf<String?>(null) }
            var dialectStatus by remember { mutableStateOf<String?>(null) }

            BackHandler(enabled = currentPage != SettingsPage.MAIN) {
                currentPage = when (currentPage) {
                    SettingsPage.LICENSE_DETAIL -> SettingsPage.ABOUT_LICENSES
                    SettingsPage.DIALECT_DETAIL -> SettingsPage.DIALECT_STORE
                    else -> SettingsPage.MAIN
                }
            }

            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    val json = preferences.exportToJson(clips, tasks)
                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                }.onSuccess {
                    backupStatus = "Your keyboard settings and history have been saved."
                }.onFailure { err ->
                    backupStatus = "Could not save backup: ${err.message}"
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read selected file")
                    val (restoredClips, restoredTasks) = preferences.importFromJsonWithTodos(json).getOrThrow()
                    clipboardHistory.restorePinnedClips(restoredClips)
                    todoManager.restoreTasks(restoredTasks)
                }.onSuccess {
                    backupStatus = "Backup restored cleanly."
                }.onFailure { err ->
                    backupStatus = "Could not restore backup: ${err.message}"
                }
            }

            val importDialectLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read selected pack")
                    dialectManager.importCustomPack(raw).getOrThrow()
                }.onSuccess { pack ->
                    dialectStatus = "Installed \"${pack.name}\" (${pack.wordCount} words) and enabled it on your keyboard!"
                }.onFailure { err ->
                    dialectStatus = "Could not import pack: ${err.message}"
                }
            }

            val exportDialectTemplateLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    val template = DialectPackManager.createStarterTemplate()
                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(template) }
                }.onSuccess {
                    dialectStatus = "Starter template saved! Edit it with your words, then tap Import to test or submit it."
                }.onFailure { err ->
                    dialectStatus = "Could not create template: ${err.message}"
                }
            }

            val licenseArticles = remember {
                listOf(
                    LicenseArticle(
                        title = "Author & Copyright Notice",
                        holder = "© 2026 Thamnis · Apache License 2.0",
                        summary = "Original authorship and distribution rights for th-kb.",
                        body = """
                            th-kb (Version ${BuildConfig.VERSION_NAME})
                            Copyright © 2026 Thamnis.

                            You are free to download and use th-kb on your personal Android device, as well as create and share custom Dialect & Jargon Packs with the community.

                            Source code and original assets authored by Thamnis are licensed under the Apache License, Version 2.0:
                            - You may use, copy, modify, and distribute this software for personal or community use.
                            - Any redistribution must preserve copyright notices and this Apache-2.0 attribution notice.
                            - Official website downloads include checksum verification to guarantee your app comes directly from the author.
                        """.trimIndent(),
                    ),
                    LicenseArticle(
                        title = "Offline Privacy Promise",
                        holder = "100% On-Device · Zero Cloud Telemetry",
                        summary = "How th-kb protects every word you type.",
                        body = """
                            Your privacy is foundational to th-kb:

                            1. Zero Internet Access: th-kb does not ask for or use any network or internet permission. It is physically impossible for the keyboard to transmit what you type anywhere.
                            2. On-Demand Microphone Only: Microphone permission is only requested if you tap Voice Typing, prefers Android's on-device recognizer, and never listens in the background.
                            3. Local Storage Only: Your learned words, dialect packs, clipboard pins, and to-do items stay strictly inside private storage on your own phone.
                            4. Password Safety: Passwords, banking PINs, and private numeric fields are never learned or suggested.
                            5. Full Control: You can export, inspect, edit, or completely erase your personal words at any time.
                        """.trimIndent(),
                    ),
                    LicenseArticle(
                        title = "English & French Dictionaries",
                        holder = "Hermit Dave (FrequencyWords) & OpenSubtitles",
                        summary = "MIT License & Creative Commons Attribution-ShareAlike 4.0",
                        body = """
                            Base conversational English and French vocabulary wordlists in th-kb are derived from Hermit Dave's FrequencyWords dataset, compiled from OpenSubtitles conversational corpora.

                            Original Repository: https://github.com/hermitdave/FrequencyWords
                            Corpus Source: OpenSubtitles (https://www.opensubtitles.org/)
                            Licenses: MIT License and CC BY-SA 4.0.

                            We gratefully acknowledge OpenSubtitles and Hermit Dave for making open linguistic frequency datasets accessible to privacy-respecting open-source tools.
                        """.trimIndent(),
                    ),
                    LicenseArticle(
                        title = "Emoji & Unicode Symbols",
                        holder = "Unicode Consortium · Unicode License V3",
                        summary = "Standardized emoji categories, flags, and character tables.",
                        body = """
                            All emoji categories, regional flags, and Unicode symbols included in th-kb conform to Unicode Standard and Unicode CLDR specifications.

                            Copyright © Unicode, Inc.
                            Licensed under the Unicode License V3 for Data Files and Software (https://www.unicode.org/license.html).
                        """.trimIndent(),
                    ),
                    LicenseArticle(
                        title = "Android & Open-Source Components",
                        holder = "Google AndroidX, Rust Foundation & Community Contributors",
                        summary = "Apache License 2.0 / MIT / MPL-2.0",
                        body = """
                            th-kb is built with open-source components:
                            - AndroidX, Jetpack Compose & Material 3 (Apache License 2.0) — Google LLC / The Android Open Source Project
                            - Community Dialect & Jargon Packs (Nouchi, Medical, Computer Science, Abbreviations) — Apache License 2.0
                            - Serialization & Bridge Components (serde, serde_json, jni, uniffi, JNA) — Dual MIT / Apache-2.0 / MPL-2.0 / LGPL-2.1.
                        """.trimIndent(),
                    ),
                )
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = SettingsAccentBlue,
                    background = SettingsCanvasBg,
                    surface = SettingsCardBg,
                    onBackground = SettingsTextPrimary,
                    onSurface = SettingsTextPrimary,
                ),
            ) {
                Scaffold(
                    containerColor = SettingsCanvasBg,
                    topBar = {
                        val activeDialectName = dialectState.availablePacks
                            .find { it.id == selectedDialectPackId }
                            ?.name
                        SettingsTopBar(
                            title = when (currentPage) {
                                SettingsPage.LICENSE_DETAIL -> selectedLicense?.title ?: "License Details"
                                SettingsPage.DIALECT_DETAIL -> activeDialectName ?: "Dialect Vocabulary"
                                else -> currentPage.title
                            },
                            canNavigateBack = currentPage != SettingsPage.MAIN,
                            onBack = {
                                currentPage = when (currentPage) {
                                    SettingsPage.LICENSE_DETAIL -> SettingsPage.ABOUT_LICENSES
                                    SettingsPage.DIALECT_DETAIL -> SettingsPage.DIALECT_STORE
                                    else -> SettingsPage.MAIN
                                }
                            },
                        )
                    },
                ) { padding ->
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeIn(tween(180)))
                                    .togetherWith(
                                        slideOutHorizontally(tween(200)) { -it / 5 } +
                                            fadeOut(tween(140)),
                                    )
                            } else {
                                (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 4 } +
                                    fadeIn(tween(180)))
                                    .togetherWith(
                                        slideOutHorizontally(tween(200)) { it / 3 } +
                                            fadeOut(tween(140)),
                                    )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        label = "settingsNavigation",
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            when (page) {
                                SettingsPage.MAIN -> {
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEAECEF)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "th-kb · Privacy-First Keyboard",
                                                    color = SettingsTextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (settings.layoutId == "azerty_fr") {
                                                        "French AZERTY · Glide & Voice Ready"
                                                    } else {
                                                        "English QWERTY · Glide & Voice Ready"
                                                    },
                                                    color = SettingsTextSecondary,
                                                    fontSize = 12.5.sp,
                                                )
                                            }
                                            Surface(
                                                color = SettingsPillSoftBlue,
                                                shape = RoundedCornerShape(50),
                                            ) {
                                                Text(
                                                    text = "100% Offline",
                                                    color = SettingsAccentBlue,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                )
                                            }
                                        }
                                    }

                                    SettingsGroup(title = "Getting Started") {
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Keyboard,
                                            title = "Turn On Keyboard",
                                            subtitle = "Enable th-kb in your phone keyboard list",
                                            onClick = {
                                                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                            },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.TouchApp,
                                            title = "Try Keyboard",
                                            subtitle = "Test typing, dialects, and suggestions live",
                                            onClick = { currentPage = SettingsPage.TRY_KEYBOARD },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Language,
                                            title = "Language & Layout",
                                            valueText = if (settings.layoutId == "azerty_fr") "French AZERTY" else "English QWERTY",
                                            onClick = { currentPage = SettingsPage.LAYOUT },
                                        )
                                    }

                                    SettingsGroup(title = "Typing & Comfort") {
                                        SettingsSwitchItem(
                                            icon = Icons.Outlined.Keyboard,
                                            title = "Auto-Capitalize Sentences",
                                            checked = settings.autoCapOnOpen,
                                            onCheckedChange = { preferences.setAutoCapOnOpen(it) },
                                        )
                                        SettingsRowDivider()
                                        SettingsSwitchItem(
                                            icon = Icons.Outlined.Visibility,
                                            title = "Show Key Popups",
                                            checked = settings.showKeyPreview,
                                            onCheckedChange = { preferences.setShowKeyPreview(it) },
                                        )
                                        SettingsRowDivider()
                                        SettingsSwitchItem(
                                            icon = Icons.Outlined.Vibration,
                                            title = "Vibrate on Keypress",
                                            checked = settings.hapticsEnabled,
                                            onCheckedChange = { preferences.setHapticsEnabled(it) },
                                        )
                                        SettingsRowDivider()
                                        SettingsSwitchItem(
                                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                            title = "Keypress Sound",
                                            checked = settings.soundEnabled,
                                            onCheckedChange = { preferences.setSoundEnabled(it) },
                                        )
                                        SettingsRowDivider()
                                        SettingsSwitchItem(
                                            icon = Icons.AutoMirrored.Outlined.Backspace,
                                            title = "Swipe to Delete Gestures",
                                            subtitle = "Swipe on Delete for forward, word, or line erase",
                                            checked = settings.deleteGesturesEnabled,
                                            onCheckedChange = { preferences.setDeleteGesturesEnabled(it) },
                                        )
                                    }

                                    SettingsGroup(title = "Words & Dialects") {
                                        SettingsSwitchItem(
                                            icon = Icons.Outlined.AutoAwesome,
                                            title = "Smart Word Suggestions",
                                            subtitle = "Helpful predictions while you type",
                                            checked = settings.suggestionsEnabled,
                                            onCheckedChange = { preferences.setSuggestionsEnabled(it) },
                                        )
                                        SettingsRowDivider()
                                        SettingsSwitchItem(
                                            icon = Icons.Outlined.Gesture,
                                            title = "Glide Typing",
                                            subtitle = "Slide across letters & Space to glide words or whole sentences",
                                            checked = settings.glideTypingEnabled,
                                            onCheckedChange = { preferences.setGlideTypingEnabled(it) },
                                        )
                                        if (settings.glideTypingEnabled) {
                                            SettingsRowDivider()
                                            SettingsSwitchItem(
                                                icon = Icons.Outlined.TouchApp,
                                                title = "Char-by-Char Glide Mode",
                                                subtitle = "Type custom words & sentences letter-by-letter as you glide",
                                                checked = settings.glideCharByChar,
                                                onCheckedChange = { preferences.setGlideCharByChar(it) },
                                            )
                                        }
                                        SettingsRowDivider()
                                        val enabledDialectCount = dialectState.enabledPackIds.count { id ->
                                            dialectState.availablePacks.any { it.id == id }
                                        }
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Storefront,
                                            title = "Dialect & Jargon Store",
                                            subtitle = "Local slangs, abbreviations, medical & tech packs",
                                            valueText = "$enabledDialectCount active",
                                            onClick = { currentPage = SettingsPage.DIALECT_STORE },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Book,
                                            title = "My Custom Words",
                                            subtitle = "Personal words & hidden suggestions",
                                            valueText = "${personalState.words.size} saved",
                                            onClick = { currentPage = SettingsPage.PERSONAL_DICT },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Mic,
                                            title = "Offline Voice Packs",
                                            subtitle = "Manage system speech recognition & offline voice languages",
                                            onClick = {
                                                val voiceIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                                runCatching { startActivity(voiceIntent) }
                                                    .onFailure {
                                                        runCatching {
                                                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                                        }
                                                    }
                                            },
                                        )
                                    }

                                    SettingsGroup(title = "Backup & About") {
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Backup,
                                            title = "Backup & Restore",
                                            subtitle = "Save or restore your settings, clips & tasks",
                                            onClick = { currentPage = SettingsPage.BACKUP },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Gavel,
                                            title = "Author Rights & Licenses",
                                            valueText = "v${BuildConfig.VERSION_NAME}",
                                            onClick = { currentPage = SettingsPage.ABOUT_LICENSES },
                                        )
                                    }
                                }

                                SettingsPage.TRY_KEYBOARD -> {
                                    SettingsGroup(title = "Interactive Sandbox") {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            OutlinedTextField(
                                                value = testText,
                                                onValueChange = { testText = it },
                                                label = { Text("Tap here to test typing, gliding & voice") },
                                                modifier = Modifier.fillMaxWidth(),
                                                minLines = 3,
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "Active: ${if (settings.layoutId == "azerty_fr") "French AZERTY" else "English QWERTY"}",
                                                    fontSize = 12.5.sp,
                                                    color = SettingsTextSecondary,
                                                )
                                                if (testText.isNotEmpty()) {
                                                    OutlinedButton(
                                                        onClick = { testText = "" },
                                                        shape = RoundedCornerShape(10.dp),
                                                    ) {
                                                        Text("Clear Box", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    SettingsGroup(title = "Quick Gestures Guide") {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "• Sentence Glide: Slide across letters, touch Spacebar, and keep sliding the next word.\n" +
                                                    "• Glide Up on Shift (⇧): Switch between Word Glide and Char-by-Char Glide.\n" +
                                                    "• Hold Spacebar (1s): Switch between French AZERTY & English QWERTY.\n" +
                                                    "• Slide Spacebar: Move the cursor left/right or up/down with the 2D dot indicator.\n" +
                                                    "• Hold Emoji (☺): Reveal hidden tools (Clipboard, To-Do List, Settings).\n" +
                                                    "• Glide Toolbar (← / →): Swipe left or right anywhere on the top toolbar to Undo or Redo.\n" +
                                                    "• Shift on Selection: Select text and tap Shift (⇧) to cycle its case.\n" +
                                                    "• Glide Up on ?123 / ABC: Switch between Numpad (123) and Symbols (?#).",
                                                fontSize = 13.sp,
                                                color = SettingsTextSecondary,
                                                lineHeight = 19.sp,
                                            )
                                        }
                                    }
                                }

                                SettingsPage.LAYOUT -> {
                                    SettingsGroup(title = "Primary Keyboard Layout") {
                                        val layouts = listOf(
                                            "qwerty_en" to "English (QWERTY)",
                                            "azerty_fr" to "French (AZERTY)",
                                        )
                                        layouts.forEachIndexed { index, (id, label) ->
                                            if (index > 0) SettingsRowDivider()
                                            SettingsCheckRow(
                                                icon = Icons.Outlined.Language,
                                                title = label,
                                                selected = settings.layoutId == id,
                                                onClick = { preferences.setLayoutId(id) },
                                            )
                                        }
                                    }
                                }

                                SettingsPage.DIALECT_STORE -> {
                                    var selectedCategory by remember { mutableStateOf("All") }
                                    val categories = remember(dialectState.availablePacks) {
                                        listOf("All") + dialectState.availablePacks.map { it.category }.distinct()
                                    }
                                    val filteredPacks = remember(dialectState.availablePacks, selectedCategory) {
                                        if (selectedCategory == "All") {
                                            dialectState.availablePacks
                                        } else {
                                            dialectState.availablePacks.filter { it.category == selectedCategory }
                                        }
                                    }

                                    Surface(
                                        color = SettingsPillSoftBlue,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "Install jargon packs to use alongside French & English. Tap any pack to explore all its words & expressions, or flip its switch to enable it!",
                                            color = SettingsTextPrimary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(14.dp),
                                        )
                                    }

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        categories.forEach { cat ->
                                            InputChip(
                                                selected = selectedCategory == cat,
                                                onClick = { selectedCategory = cat },
                                                label = { Text(cat, fontSize = 12.5.sp) },
                                            )
                                        }
                                    }

                                    SettingsGroup(title = "Available Jargon & Dialect Packs (${filteredPacks.size}) · Tap to View Words") {
                                        filteredPacks.forEachIndexed { idx, pack ->
                                            if (idx > 0) SettingsRowDivider()
                                            val isEnabled = pack.id in dialectState.enabledPackIds
                                            DialectPackRow(
                                                pack = pack,
                                                enabled = isEnabled,
                                                onToggle = { next -> dialectManager.togglePack(pack.id, next) },
                                                onInspectWords = {
                                                    selectedDialectPackId = pack.id
                                                    dialectWordSearch = ""
                                                    currentPage = SettingsPage.DIALECT_DETAIL
                                                },
                                                onDeleteCustom = if (!pack.isBuiltIn) {
                                                    { dialectManager.removeCustomPack(pack.id) }
                                                } else {
                                                    null
                                                },
                                            )
                                        }
                                    }

                                    SettingsGroup(title = "Community & Custom Dialects") {
                                        SettingsNavRow(
                                            icon = Icons.Outlined.UploadFile,
                                            title = "Import Dialect Pack",
                                            subtitle = "Load a community or custom dialect file from your phone",
                                            onClick = {
                                                importDialectLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                            },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Add,
                                            title = "Create & Export Template",
                                            subtitle = "Save a blank starter file to build your own dialect & share",
                                            onClick = {
                                                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                                exportDialectTemplateLauncher.launch("thkb_dialect_template_$stamp.json")
                                            },
                                        )
                                    }

                                    if (dialectStatus != null) {
                                        SettingsGroup(title = "Store Status") {
                                            Text(
                                                text = dialectStatus!!,
                                                fontSize = 13.sp,
                                                color = SettingsAccentBlue,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                        }
                                    }
                                }

                                SettingsPage.DIALECT_DETAIL -> {
                                    val pack = dialectState.availablePacks
                                        .find { it.id == selectedDialectPackId }
                                        ?: dialectState.availablePacks.firstOrNull()
                                    if (pack != null) {
                                        val isEnabled = pack.id in dialectState.enabledPackIds
                                        val query = dialectWordSearch.trim().lowercase(Locale.ROOT)
                                        val filteredWords = remember(pack.allWords, query) {
                                            if (query.isEmpty()) {
                                                pack.allWords
                                            } else {
                                                pack.allWords.filter { it.lowercase(Locale.ROOT).contains(query) }
                                            }
                                        }
                                        val filteredPhrases = remember(pack.samplePhrases, query) {
                                            if (query.isEmpty()) {
                                                pack.samplePhrases
                                            } else {
                                                pack.samplePhrases.filter { it.lowercase(Locale.ROOT).contains(query) }
                                            }
                                        }

                                        SettingsGroup(title = "${pack.category} · ${pack.wordCount} Words") {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = pack.name,
                                                            color = SettingsTextPrimary,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                        Text(
                                                            text = "By ${pack.author} · v${pack.version}",
                                                            color = SettingsAccentBlue,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                        )
                                                    }
                                                    Switch(
                                                        checked = isEnabled,
                                                        onCheckedChange = { next ->
                                                            dialectManager.togglePack(pack.id, next)
                                                        },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = Color.White,
                                                            checkedTrackColor = SettingsSwitchGreen,
                                                            uncheckedThumbColor = Color.White,
                                                            uncheckedTrackColor = Color(0xFFE5E7EB),
                                                            uncheckedBorderColor = Color.Transparent,
                                                        ),
                                                    )
                                                }
                                                Text(
                                                    text = pack.description,
                                                    color = SettingsTextSecondary,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                )
                                                OutlinedTextField(
                                                    value = dialectWordSearch,
                                                    onValueChange = { dialectWordSearch = it },
                                                    label = { Text("Search words & expressions in ${pack.name}") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }

                                        if (filteredPhrases.isNotEmpty()) {
                                            SettingsGroup(title = "Common Expressions & Phrases (${filteredPhrases.size})") {
                                                FlowRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    filteredPhrases.forEach { phrase ->
                                                        Surface(
                                                            color = SettingsPillSoftBlue,
                                                            shape = RoundedCornerShape(10.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                                                        ) {
                                                            Text(
                                                                text = phrase,
                                                                color = SettingsAccentBlue,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        SettingsGroup(title = "Vocabulary Words (${filteredWords.size})") {
                                            if (filteredWords.isEmpty()) {
                                                Text(
                                                    text = "No words match \"$dialectWordSearch\".",
                                                    color = SettingsTextSecondary,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(16.dp),
                                                )
                                            } else {
                                                FlowRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    filteredWords.forEach { word ->
                                                        Surface(
                                                            color = SettingsCanvasBg,
                                                            shape = RoundedCornerShape(10.dp),
                                                            border = BorderStroke(1.dp, SettingsBorderColor),
                                                        ) {
                                                            Text(
                                                                text = word,
                                                                color = SettingsTextPrimary,
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                SettingsPage.PERSONAL_DICT -> {
                                    SettingsGroup(title = "Add a Custom Word or Name") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            OutlinedTextField(
                                                value = newCustomWord,
                                                onValueChange = { newCustomWord = it },
                                                label = { Text("Enter word or name") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Button(
                                                onClick = {
                                                    val clean = newCustomWord.trim()
                                                    if (clean.length >= 2) {
                                                        personalDictionary.addCustomWord(clean)
                                                        newCustomWord = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SettingsAccentBlue),
                                            ) {
                                                Text("Add")
                                            }
                                        }
                                    }

                                    if (personalState.words.isNotEmpty()) {
                                        SettingsGroup(title = "My Saved & Learned Words (${personalState.words.size}) · Tap to Remove") {
                                            FlowRow(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                personalState.words.entries
                                                    .sortedByDescending { it.value }
                                                    .take(30)
                                                    .forEach { (word, _) ->
                                                        InputChip(
                                                            selected = false,
                                                            onClick = { personalDictionary.removeWord(word) },
                                                            label = { Text("$word ×") },
                                                        )
                                                    }
                                            }
                                        }
                                    }

                                    if (personalState.blocked.isNotEmpty()) {
                                        SettingsGroup(title = "Hidden Words (${personalState.blocked.size}) · Tap to Restore") {
                                            FlowRow(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                personalState.blocked.forEach { word ->
                                                    InputChip(
                                                        selected = false,
                                                        onClick = { personalDictionary.unblockWord(word) },
                                                        label = { Text("$word ↺") },
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (personalState.words.isNotEmpty() || personalState.blocked.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = { personalDictionary.clearLearned() },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text("Reset Personal Dictionary")
                                        }
                                    }
                                }

                                SettingsPage.BACKUP -> {
                                    SettingsGroup(title = "Keyboard Backup") {
                                        SettingsNavRow(
                                            icon = Icons.Outlined.Backup,
                                            title = "Save Backup File",
                                            subtitle = "Export your preferences, pinned clips, and to-do tasks",
                                            onClick = {
                                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                                exportLauncher.launch("thkb_backup_$stamp.json")
                                            },
                                        )
                                        SettingsRowDivider()
                                        SettingsNavRow(
                                            icon = Icons.Outlined.UploadFile,
                                            title = "Restore Backup File",
                                            subtitle = "Import a saved backup from your phone",
                                            onClick = {
                                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                            },
                                        )
                                    }

                                    if (backupStatus != null) {
                                        SettingsGroup(title = "Status") {
                                            Text(
                                                text = backupStatus!!,
                                                fontSize = 13.sp,
                                                color = SettingsAccentBlue,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                        }
                                    }
                                }

                                SettingsPage.ABOUT_LICENSES -> {
                                    SettingsGroup(title = "th-kb · Tap Any Item to Read") {
                                        licenseArticles.take(2).forEachIndexed { idx, article ->
                                            if (idx > 0) SettingsRowDivider()
                                            SettingsNavRow(
                                                icon = if (idx == 0) Icons.Outlined.Gavel else Icons.Outlined.Lock,
                                                title = article.title,
                                                subtitle = article.holder,
                                                onClick = {
                                                    selectedLicense = article
                                                    currentPage = SettingsPage.LICENSE_DETAIL
                                                },
                                            )
                                        }
                                    }

                                    SettingsGroup(title = "Open-Source Dictionaries & Libraries · Tap to Read") {
                                        licenseArticles.drop(2).forEachIndexed { idx, article ->
                                            if (idx > 0) SettingsRowDivider()
                                            SettingsNavRow(
                                                icon = Icons.Outlined.Book,
                                                title = article.title,
                                                subtitle = article.holder,
                                                onClick = {
                                                    selectedLicense = article
                                                    currentPage = SettingsPage.LICENSE_DETAIL
                                                },
                                            )
                                        }
                                    }
                                }

                                SettingsPage.LICENSE_DETAIL -> {
                                    val article = selectedLicense
                                    if (article != null) {
                                        SettingsGroup(title = article.holder) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Text(
                                                    text = article.summary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SettingsAccentBlue,
                                                )
                                                HorizontalDivider(color = SettingsDividerColor)
                                                Text(
                                                    text = article.body,
                                                    fontSize = 13.5.sp,
                                                    lineHeight = 20.sp,
                                                    color = SettingsTextPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialectPackRow(
    pack: DialectPack,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onInspectWords: () -> Unit,
    onDeleteCustom: (() -> Unit)?,
) {
    val icon = when {
        pack.id.contains("nouchi") || pack.category.contains("Regional") -> Icons.Outlined.Public
        pack.id.contains("tech") -> Icons.Outlined.Code
        pack.id.contains("medicine") -> Icons.Outlined.LocalHospital
        pack.category.contains("Abbreviation") -> Icons.Outlined.Tag
        else -> Icons.AutoMirrored.Outlined.Chat
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspectWords)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) SettingsAccentBlue else SettingsIconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = pack.name,
                    color = SettingsTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${pack.category} · ${pack.wordCount} words",
                color = SettingsAccentBlue,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = pack.description,
                color = SettingsTextSecondary,
                fontSize = 12.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (pack.sampleWords.isNotEmpty()) {
                Text(
                    text = pack.sampleWords.joinToString(" · "),
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "View all ${pack.wordCount} words →",
                color = SettingsAccentBlue,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (onDeleteCustom != null) {
            IconButton(onClick = onDeleteCustom) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove custom pack",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SettingsSwitchGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
) {
    Surface(
        color = SettingsCanvasBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        ) {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = SettingsTextPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Text(
                text = title,
                color = SettingsTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = SettingsTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        Surface(
            color = SettingsCardBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SettingsBorderColor),
            shadowElevation = 0.5.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        color = SettingsDividerColor,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
    )
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    valueText: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SettingsIconTint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsTextPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SettingsTextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (valueText != null) {
            Text(
                text = valueText,
                color = SettingsTextSecondary,
                fontSize = 13.5.sp,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SettingsIconTint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsTextPrimary,
                fontSize = 15.sp,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SettingsTextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SettingsSwitchGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SettingsCheckRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) SettingsAccentBlue else SettingsIconTint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SettingsTextPrimary,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SettingsTextSecondary,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = SettingsAccentBlue,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
