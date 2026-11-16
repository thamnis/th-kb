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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = KbPreferences(this)

        setContent {
            val settings by preferences.settings.collectAsState()

            MaterialTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                                    .padding(vertical = 4.dp),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Haptic feedback on keypress",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Switch(
                                checked = settings.hapticsEnabled,
                                onCheckedChange = { preferences.setHapticsEnabled(it) },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Sound on keypress",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Switch(
                                checked = settings.soundEnabled,
                                onCheckedChange = { preferences.setSoundEnabled(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
