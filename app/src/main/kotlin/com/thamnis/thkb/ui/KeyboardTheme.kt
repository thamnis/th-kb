package com.thamnis.thkb.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val BgColor = Color(0xFF1B1C1E)
internal val KeyBgColor = Color(0xFF3A3C41)
internal val KeyShadowColor = Color(0xFF0E0F11)
internal val KeyPressedColor = Color(0xFF5A5D64)
internal val ActionKeyBgColor = Color(0xFF2B2D31)
internal val PillKeyBgColor = Color(0xFF43474E)
internal val ReturnBgColor = Color(0xFFA8C7FA)
internal val ReturnFgColor = Color(0xFF062E6F)
internal val HintTextColor = Color(0xFF9AA0A6)
internal val ToolbarTextColor = Color(0xFFE3E3E3)

@Composable
internal fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) ReturnBgColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "toolbarBtnBg",
    )
    val animatedTint by animateColorAsState(
        targetValue = if (active) ReturnFgColor else ToolbarTextColor,
        animationSpec = tween(durationMillis = 180),
        label = "toolbarBtnTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toolbarBtnScale",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

@Composable
internal fun CategoryChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) ReturnBgColor else ActionKeyBgColor,
        animationSpec = tween(durationMillis = 180),
        label = "chipBg",
    )
    val animatedFg by animateColorAsState(
        targetValue = if (active) ReturnFgColor else ToolbarTextColor,
        animationSpec = tween(durationMillis = 180),
        label = "chipFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = animatedFg,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
