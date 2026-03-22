package com.sayaem.nebula.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.sayaem.nebula.ui.theme.*

// ─── Sleep Timer State ───────────────────────────────────────────────
data class SleepTimerState(
    val isActive: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
) {
    val progress: Float get() =
        if (totalSeconds == 0) 0f else 1f - (remainingSeconds.toFloat() / totalSeconds)

    val remainingFormatted: String get() {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        return "${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
    }
}

// ─── Sleep Timer Sheet ───────────────────────────────────────────────
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DarkSurface)
                .clickable(enabled = false) {}
                .padding(24.dp),
        ) {
            Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                .background(DarkBorder).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = NebulaCyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))

            if (state.isActive) {
                // Countdown ring
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { state.progress },
                            modifier = Modifier.size(160.dp), strokeWidth = 8.dp,
                            color = NebulaCyan, trackColor = DarkBorder)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.remainingFormatted,
                                style = MaterialTheme.typography.displaySmall,
                                color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                            Text("remaining", style = MaterialTheme.typography.bodySmall,
                                color = TextTertiaryDark)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(NebulaRed.copy(alpha = 0.1f))
                    .border(1.dp, NebulaRed.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable { onCancel(); onDismiss() }
                    .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Cancel Timer", style = MaterialTheme.typography.labelLarge, color = NebulaRed)
                }
            } else {
                // Duration options
                val options = listOf(5, 10, 15, 20, 30, 45, 60, 90)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { mins ->
                                Box(
                                    modifier = Modifier.weight(1f).height(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NebulaCyan.copy(alpha = 0.08f))
                                        .border(0.5.dp, NebulaCyan.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                        .clickable { onStart(mins); onDismiss() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$mins", style = MaterialTheme.typography.headlineSmall,
                                            color = NebulaCyan, fontWeight = FontWeight.Bold)
                                        Text("min", style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiaryDark)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = TextTertiaryDark, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Music fades out gracefully before stopping",
                        style = MaterialTheme.typography.bodySmall, color = TextTertiaryDark)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Speed Picker Sheet ──────────────────────────────────────────────
@Composable
fun SpeedPickerSheet(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DarkSurface)
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                .background(DarkBorder).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, null, tint = NebulaAmber, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Playback Speed", style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryDark, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                speeds.forEach { speed ->
                    val sel = kotlin.math.abs(speed - current) < 0.01f
                    Box(
                        modifier = Modifier.weight(1f).height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) NebulaAmber.copy(alpha = 0.2f)
                                else DarkCard)
                            .border(if (sel) 1.dp else 0.5.dp,
                                if (sel) NebulaAmber.copy(alpha = 0.5f) else DarkBorder,
                                RoundedCornerShape(12.dp))
                            .clickable { onSelect(speed); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${speed}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) NebulaAmber else TextSecondaryDark,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
