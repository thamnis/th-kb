package com.thamnis.thkb.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.ime.VoiceUiState

@Composable
internal fun VoiceTypingPanel(
    voiceUiState: VoiceUiState,
    onStartOrStop: () -> Unit,
    onToggleLanguage: () -> Unit,
    onLaunchSystemVoice: () -> Unit,
    onBackToKeyboard: () -> Unit,
    onBackspace: () -> Unit,
) {
    val normRms = (voiceUiState.rmsDb / 10f).coerceIn(0f, 1f)
    val animatedRms by animateFloatAsState(
        targetValue = if (voiceUiState.isListening) normRms else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "voiceRms",
    )
    val langLabel = if (voiceUiState.languageTag.startsWith("fr", ignoreCase = true)) "FR" else "EN"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top header strip: System Voice fallback button + clickable language toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = ActionKeyBgColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onLaunchSystemVoice),
            ) {
                Text(
                    text = "System Voice ↗",
                    color = ReturnBgColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
            Surface(
                color = PillKeyBgColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleLanguage),
            ) {
                Text(
                    text = if (langLabel == "FR") "Français (FR)" else "English (EN)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        // Center live waveform + transcript / error prompt
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val weights = listOf(0.45f, 0.75f, 1.0f, 0.85f, 1.0f, 0.7f, 0.45f)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(44.dp),
            ) {
                weights.forEach { w ->
                    val barHeight = if (voiceUiState.isListening) {
                        (10f + 30f * w * (0.28f + 0.72f * animatedRms)).dp
                    } else {
                        8.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(barHeight)
                            .background(
                                color = if (voiceUiState.isListening) ReturnBgColor else HintTextColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(50),
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    voiceUiState.errorMessage != null -> voiceUiState.errorMessage
                    voiceUiState.partialText.isNotEmpty() -> voiceUiState.partialText
                    voiceUiState.isListening -> "Speak now ($langLabel)..."
                    else -> "Tap microphone to dictate"
                },
                color = when {
                    voiceUiState.errorMessage != null -> Color(0xFFFCA5A5)
                    voiceUiState.partialText.isNotEmpty() -> Color.White
                    else -> HintTextColor
                },
                fontSize = if (voiceUiState.partialText.isNotEmpty()) 15.sp else 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Bottom control row matching keyboard bottom aesthetics: ABC | Mic Orb | Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PillKeyBgColor)
                    .clickable(onClick = onBackToKeyboard)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "ABC",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (voiceUiState.isListening) ReturnBgColor else KeyBgColor)
                    .clickable(onClick = onStartOrStop),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        voiceUiState.isListening -> Icons.Outlined.Stop
                        voiceUiState.errorMessage != null -> Icons.Outlined.MicOff
                        else -> Icons.Outlined.Mic
                    },
                    contentDescription = if (voiceUiState.isListening) "Stop & Insert" else "Start Dictation",
                    tint = if (voiceUiState.isListening) ReturnFgColor else Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PillKeyBgColor)
                    .clickable(onClick = onBackspace),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
