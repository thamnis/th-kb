package com.thamnis.thkb.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = KbPreferences(this)

        setContent {
            val settings by preferences.settings.collectAsState()
            var testText by remember { mutableStateOf("") }

            MaterialTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "th-kb Settings",
                            style = MaterialTheme.typography.headlineMedium,
                        )

                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        }) {
                            Text("Enable in Input Method Settings")
                        }

                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            label = { Text("Test keyboard here") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )

                        HorizontalDivider()

                        Text(
                            text = "Keyboard Layout",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        listOf(
                            "qwerty_en" to "QWERTY (English US)",
                            "azerty_fr" to "AZERTY (French)",
                        ).forEach { (id, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { preferences.setLayoutId(id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    selected = settings.layoutId == id,
                                    onClick = { preferences.setLayoutId(id) },
                                )
                                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Typing & Feedback",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        SettingSwitchRow(
                            label = "Enable Shift when opening keyboard",
                            checked = settings.autoCapOnOpen,
                            onCheckedChange = { preferences.setAutoCapOnOpen(it) },
                        )

                        SettingSwitchRow(
                            label = "Haptic feedback on keypress",
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { preferences.setHapticsEnabled(it) },
                        )

                        SettingSwitchRow(
                            label = "Sound on keypress",
                            checked = settings.soundEnabled,
                            onCheckedChange = { preferences.setSoundEnabled(it) },
                        )

                        HorizontalDivider()

                        Text(
                            text = "Delete & Long-Press Gestures",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Text(
                            text = "Normal key long-press delay (${settings.longPressDelayMs} ms)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(350L, 450L, 600L).forEach { delayMs ->
                                FilterChip(
                                    selected = settings.longPressDelayMs == delayMs,
                                    onClick = { preferences.setLongPressDelayMs(delayMs) },
                                    label = { Text("${delayMs}ms") },
                                )
                            }
                        }

                        Text(
                            text = "Backspace very-long-press rapid delete delay (${settings.rapidDeleteDelayMs} ms)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(450L, 580L, 750L).forEach { delayMs ->
                                FilterChip(
                                    selected = settings.rapidDeleteDelayMs == delayMs,
                                    onClick = { preferences.setRapidDeleteDelayMs(delayMs) },
                                    label = { Text("${delayMs}ms") },
                                )
                            }
                        }

                        SettingSwitchRow(
                            label = "Backspace swipe gestures (Down = right-delete, Left = word, Up = paragraph)",
                            checked = settings.deleteGesturesEnabled,
                            onCheckedChange = { preferences.setDeleteGesturesEnabled(it) },
                        )

                        SettingSwitchRow(
                            label = "Reset right-delete mode after typing a character",
                            checked = settings.resetForwardDeleteOnChar,
                            onCheckedChange = { preferences.setResetForwardDeleteOnChar(it) },
                        )

                        HorizontalDivider()

                        Text(
                            text = "Emoji Display",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Text(
                            text = "Emoji grid columns (${settings.emojiGridColumns} per row)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                6 to "6 (Large)",
                                7 to "7 (Default)",
                                8 to "8 (Compact)",
                                9 to "9 (Dense)",
                            ).forEach { (cols, label) ->
                                FilterChip(
                                    selected = settings.emojiGridColumns == cols,
                                    onClick = { preferences.setEmojiGridColumns(cols) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
