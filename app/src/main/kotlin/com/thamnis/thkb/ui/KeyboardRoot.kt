package com.thamnis.thkb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyboardRoot(
    onCommitText: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    Surface(
        color = Color(0xFF1F2124),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 44dp top toolbar stub (Gboard-parity height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "th-kb v0.1",
                    color = Color(0xFF9AA0A6),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // 4 x 54dp key rows stub
            val rows = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m", "DEL"),
                listOf("SPACE"),
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .background(
                                    color = Color(0xFF3C4043),
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .clickable {
                                    when (label) {
                                        "DEL" -> onBackspace()
                                        "SPACE" -> onCommitText(" ")
                                        else -> onCommitText(label)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
