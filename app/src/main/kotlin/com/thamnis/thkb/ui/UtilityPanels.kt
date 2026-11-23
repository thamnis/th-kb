package com.thamnis.thkb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.data.CrossDevicePeer
import com.thamnis.thkb.data.CrossDeviceProtocol
import com.thamnis.thkb.data.PeerProtocol
import com.thamnis.thkb.data.RemoteInputClient
import com.thamnis.thkb.ime.MediaControlAction
import com.thamnis.thkb.ime.MediaPlayerSnapshot
import com.thamnis.thkb.ime.NavDirection
import com.thamnis.thkb.ime.NavUnit
import kotlinx.coroutines.launch

@Composable
internal fun NavigationPanel(
    panelHeightDp: Float,
    onNavigate: (NavUnit, NavDirection, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onKeyFeedback: () -> Unit,
) {
    var selecting by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(NavUnit.WORD) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Top row: Move/Select toggle + Unit pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(
                label = if (selecting) "Select: ON" else "Move",
                active = selecting,
                onClick = {
                    onKeyFeedback()
                    selecting = !selecting
                },
            )
            listOf(
                NavUnit.CHAR to "Letter",
                NavUnit.WORD to "Word",
                NavUnit.PHRASE to "Phrase",
                NavUnit.PARAGRAPH to "Para",
            ).forEach { (itemUnit, label) ->
                CategoryChip(
                    label = label,
                    active = unit == itemUnit,
                    onClick = {
                        onKeyFeedback()
                        unit = itemUnit
                    },
                )
            }
        }

        // Main D-Pad + Home/End cluster
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Left: Home / End / Doc Start / Doc End
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UtilityActionTile(
                        label = "Home",
                        icon = Icons.Outlined.FirstPage,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(NavUnit.LINE, NavDirection.START, selecting)
                        },
                    )
                    UtilityActionTile(
                        label = "End",
                        icon = Icons.AutoMirrored.Outlined.LastPage,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(NavUnit.LINE, NavDirection.END, selecting)
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UtilityActionTile(
                        label = "Top",
                        icon = Icons.Outlined.KeyboardArrowUp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(NavUnit.DOCUMENT, NavDirection.START, selecting)
                        },
                    )
                    UtilityActionTile(
                        label = "Bottom",
                        icon = Icons.Outlined.KeyboardArrowDown,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(NavUnit.DOCUMENT, NavDirection.END, selecting)
                        },
                    )
                }
            }

            // Right: Directional Cursor Pad
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UtilityActionTile(
                        label = null,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        accent = selecting,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(unit, NavDirection.LEFT, selecting)
                        },
                    )
                    UtilityActionTile(
                        label = null,
                        icon = Icons.Outlined.KeyboardArrowUp,
                        accent = selecting,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(unit, NavDirection.UP, selecting)
                        },
                    )
                    UtilityActionTile(
                        label = null,
                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                        accent = selecting,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(unit, NavDirection.RIGHT, selecting)
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UtilityActionTile(
                        label = null,
                        icon = Icons.Outlined.KeyboardArrowDown,
                        accent = selecting,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onKeyFeedback()
                            onNavigate(unit, NavDirection.DOWN, selecting)
                        },
                    )
                }
            }
        }

        // Bottom clipboard actions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            UtilityActionTile(
                label = "All",
                icon = Icons.Outlined.SelectAll,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onSelectAll()
                },
            )
            UtilityActionTile(
                label = "Copy",
                icon = Icons.Outlined.ContentCopy,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onCopy()
                },
            )
            UtilityActionTile(
                label = "Cut",
                icon = Icons.Outlined.ContentCut,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onCut()
                },
            )
            UtilityActionTile(
                label = "Paste",
                icon = Icons.Outlined.ContentPaste,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onPaste()
                },
            )
        }
    }
}

@Composable
internal fun MediaControlPanel(
    panelHeightDp: Float,
    playbackSnapshot: MediaPlayerSnapshot,
    onMediaAction: (MediaControlAction) -> Unit,
    onEnableMediaSync: () -> Unit = {},
    onKeyFeedback: () -> Unit,
) {
    var optimisticPlaying by remember(playbackSnapshot.isPlaying) {
        mutableStateOf(playbackSnapshot.isPlaying)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when {
                    playbackSnapshot.trackTitle.isNotEmpty() -> playbackSnapshot.trackTitle
                    optimisticPlaying -> "Playing Media"
                    else -> "Paused · Media Controls"
                },
                color = if (optimisticPlaying) ReturnBgColor else HintTextColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!playbackSnapshot.listenerEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                CategoryChip(
                    label = "Sync Player",
                    active = false,
                    onClick = {
                        onKeyFeedback()
                        onEnableMediaSync()
                    },
                )
            }
        }

        // Main transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UtilityRoundButton(
                icon = Icons.Outlined.FastRewind,
                description = "Rewind 10s",
                sizeDp = 44,
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.REWIND)
                },
            )
            UtilityRoundButton(
                icon = Icons.Outlined.SkipPrevious,
                description = "Previous",
                sizeDp = 48,
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.PREVIOUS)
                },
            )
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(ReturnBgColor)
                    .clickable {
                        onKeyFeedback()
                        optimisticPlaying = !optimisticPlaying
                        onMediaAction(MediaControlAction.PLAY_PAUSE)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (optimisticPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (optimisticPlaying) "Pause" else "Play",
                    tint = ReturnFgColor,
                    modifier = Modifier.size(32.dp),
                )
            }
            UtilityRoundButton(
                icon = Icons.Outlined.SkipNext,
                description = "Next",
                sizeDp = 48,
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.NEXT)
                },
            )
            UtilityRoundButton(
                icon = Icons.Outlined.FastForward,
                description = "Fast Forward 10s",
                sizeDp = 44,
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.FAST_FORWARD)
                },
            )
        }

        // System volume controls (bigger jumps per tap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UtilityActionTile(
                label = "Vol --",
                icon = Icons.AutoMirrored.Outlined.VolumeDown,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.VOL_DOWN)
                },
            )
            UtilityActionTile(
                label = "Mute",
                icon = Icons.AutoMirrored.Outlined.VolumeOff,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.MUTE)
                },
            )
            UtilityActionTile(
                label = "Vol ++",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    onKeyFeedback()
                    onMediaAction(MediaControlAction.VOL_UP)
                },
            )
        }
    }
}

@Composable
internal fun RemoteControlPanel(
    panelHeightDp: Float,
    savedHostIp: String,
    savedHostPort: Int,
    latestClipText: String = "",
    onSelectHost: (String, Int) -> Unit,
    onOpenCrossDeviceSetup: () -> Unit = {},
    onKeyFeedback: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var discoveredPeers by remember { mutableStateOf<List<CrossDevicePeer>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    var activeHost by remember(savedHostIp) { mutableStateOf(savedHostIp) }
    var activePort by remember(savedHostPort) { mutableStateOf(savedHostPort) }
    var ipInputDraft by remember(savedHostIp) { mutableStateOf(savedHostIp) }
    var verifiedName by remember { mutableStateOf<String?>(null) }
    var showConnectDrawer by remember { mutableStateOf(savedHostIp.isBlank()) }

    fun scanAndVerify() {
        if (isScanning) return
        isScanning = true
        scope.launch {
            if (activeHost.isNotBlank()) {
                verifiedName = CrossDeviceProtocol.pingRemoteCompanion(activeHost, activePort)
            }
            val peers = CrossDeviceProtocol.discoverAllPeers(context, 1300L)
                .filter { it.protocol == PeerProtocol.THKB_REMOTE }
            discoveredPeers = peers
            if (peers.isNotEmpty() && (activeHost.isBlank() || verifiedName == null)) {
                val chosen = peers.first()
                activeHost = chosen.ip
                activePort = chosen.port
                ipInputDraft = chosen.ip
                verifiedName = chosen.name
                onSelectHost(chosen.ip, chosen.port)
                showConnectDrawer = false
            }
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        scanAndVerify()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Intuitive Connection Bar: Live Green Verified Badge + Tap to Connect/Switch PC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ActionKeyBgColor)
                    .clickable {
                        onKeyFeedback()
                        showConnectDrawer = !showConnectDrawer
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                verifiedName != null -> Color(0xFF4ADE80)
                                isScanning -> Color(0xFFFBBF24)
                                else -> HintTextColor
                            },
                        ),
                )
                Text(
                    text = when {
                        verifiedName != null -> "$verifiedName ($activeHost)"
                        isScanning -> "Searching PC on Wi-Fi..."
                        activeHost.isNotEmpty() -> "$activeHost · Tap to edit"
                        else -> "Tap to connect PC"
                    },
                    color = if (verifiedName != null) ToolbarTextColor else HintTextColor,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (latestClipText.isNotBlank() && activeHost.isNotBlank()) {
                CategoryChip(
                    label = "Send Clip",
                    active = false,
                    onClick = {
                        onKeyFeedback()
                        RemoteInputClient.sendText(activeHost, activePort, latestClipText)
                    },
                )
            }

            CategoryChip(
                label = if (isScanning) "..." else "Scan",
                active = verifiedName == null && !isScanning,
                onClick = {
                    onKeyFeedback()
                    scanAndVerify()
                },
            )
        }

        if (showConnectDrawer) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KeyBgColor)
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Run `th-kb-companion` on your PC (same Wi-Fi) or enter its IP:",
                    color = ToolbarTextColor,
                    fontSize = 11.5.sp,
                )
                if (discoveredPeers.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        discoveredPeers.take(3).forEach { peer ->
                            CategoryChip(
                                label = "${peer.name} (${peer.ip})",
                                active = activeHost == peer.ip,
                                onClick = {
                                    onKeyFeedback()
                                    activeHost = peer.ip
                                    activePort = peer.port
                                    ipInputDraft = peer.ip
                                    verifiedName = peer.name
                                    onSelectHost(peer.ip, peer.port)
                                    showConnectDrawer = false
                                },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ActionKeyBgColor)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        BasicTextField(
                            value = ipInputDraft,
                            onValueChange = { ipInputDraft = it },
                            singleLine = true,
                            textStyle = TextStyle(color = KeyTextColor, fontSize = 13.sp),
                            cursorBrush = SolidColor(ReturnBgColor),
                            decorationBox = { inner ->
                                if (ipInputDraft.isEmpty()) {
                                    Text("PC IP (e.g. 192.168.1.42)", color = HintTextColor, fontSize = 12.sp)
                                }
                                inner()
                            },
                        )
                    }
                    CategoryChip(
                        label = "Connect",
                        active = ipInputDraft.isNotBlank(),
                        onClick = {
                            val clean = ipInputDraft.trim()
                            if (clean.isNotEmpty()) {
                                onKeyFeedback()
                                activeHost = clean
                                onSelectHost(clean, activePort)
                                showConnectDrawer = false
                                scope.launch {
                                    verifiedName = CrossDeviceProtocol.pingRemoteCompanion(clean, activePort)
                                }
                            }
                        },
                    )
                    CategoryChip(
                        label = "Guide",
                        active = false,
                        onClick = {
                            onKeyFeedback()
                            onOpenCrossDeviceSetup()
                        },
                    )
                }
            }
        } else {

        // Trackpad surface + vertical scroll rail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KeyBgColor)
                    .border(1.dp, PillKeyBgColor, RoundedCornerShape(12.dp))
                    .pointerInput(activeHost, activePort) {
                        detectTapGestures(
                            onTap = {
                                onKeyFeedback()
                                RemoteInputClient.sendClick(activeHost, activePort, "left")
                            },
                            onDoubleTap = {
                                onKeyFeedback()
                                RemoteInputClient.sendClick(activeHost, activePort, "right")
                            },
                        )
                    }
                    .pointerInput(activeHost, activePort) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            RemoteInputClient.sendMove(
                                activeHost,
                                activePort,
                                dragAmount.x * 1.45f,
                                dragAmount.y * 1.45f,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (activeHost.isEmpty()) "No companion connected" else "Touchpad • Tap: Left Click • 2xTap: Right Click",
                    color = HintTextColor,
                    fontSize = 11.5.sp,
                )
            }

            // Scroll rail
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ActionKeyBgColor)
                    .pointerInput(activeHost, activePort) {
                        var accum = 0f
                        detectDragGestures(
                            onDragEnd = { accum = 0f },
                            onDragCancel = { accum = 0f },
                        ) { change, dragAmount ->
                            change.consume()
                            accum += dragAmount.y
                            if (kotlin.math.abs(accum) >= 24f) {
                                val steps = (accum / 24f).toInt()
                                accum -= steps * 24f
                                RemoteInputClient.sendScroll(activeHost, activePort, steps.toFloat())
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⇅",
                    color = HintTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Mouse buttons + quick keys row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            listOf(
                "Left" to { RemoteInputClient.sendClick(activeHost, activePort, "left") },
                "Right" to { RemoteInputClient.sendClick(activeHost, activePort, "right") },
                "Esc" to { RemoteInputClient.sendSpecialKey(activeHost, activePort, "esc") },
                "Tab" to { RemoteInputClient.sendSpecialKey(activeHost, activePort, "tab") },
                "⌫" to { RemoteInputClient.sendSpecialKey(activeHost, activePort, "backspace") },
                "⏎" to { RemoteInputClient.sendSpecialKey(activeHost, activePort, "enter") },
            ).forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PillKeyBgColor)
                        .clickable {
                            onKeyFeedback()
                            action()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = KeyTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Direct text-to-PC composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KeyBgColor)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = draftText,
                onValueChange = { draftText = it },
                singleLine = true,
                textStyle = TextStyle(color = KeyTextColor, fontSize = 13.sp),
                cursorBrush = SolidColor(ReturnBgColor),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (draftText.isEmpty()) {
                        Text(
                            text = "Send text to computer...",
                            color = HintTextColor,
                            fontSize = 12.5.sp,
                        )
                    }
                    inner()
                },
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (draftText.isNotEmpty()) ReturnBgColor else ActionKeyBgColor)
                    .clickable(enabled = draftText.isNotEmpty() && activeHost.isNotEmpty()) {
                        onKeyFeedback()
                        RemoteInputClient.sendText(activeHost, activePort, draftText)
                        draftText = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send to PC",
                    tint = if (draftText.isNotEmpty()) ReturnFgColor else HintTextColor,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun UtilityActionTile(
    label: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) ReturnBgColor else KeyBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label ?: "Action",
                tint = if (accent) ReturnFgColor else KeyTextColor,
                modifier = Modifier.size(17.dp),
            )
            if (label != null) {
                Text(
                    text = label,
                    color = if (accent) ReturnFgColor else KeyTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun UtilityRoundButton(
    icon: ImageVector,
    description: String,
    sizeDp: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(KeyBgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = KeyTextColor,
            modifier = Modifier.size((sizeDp * 0.48f).dp),
        )
    }
}
