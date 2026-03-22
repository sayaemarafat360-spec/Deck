package com.sayaem.nebula.ui.screens

import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.sayaem.nebula.MainViewModel
import com.sayaem.nebula.ui.theme.*


data class EqState(
    val preset: String = "Flat",
    val bands: List<Float> = List(10) { 0f },
    val enabled: Boolean = true,
    val bassBoost: Float = 0f,
)

@Composable
fun EqualizerScreen(
    eqState: EqState,
    onBandChanged: (Int, Float) -> Unit,
    onPresetChanged: (String) -> Unit,
    onToggleEq: () -> Unit,
    onBack: () -> Unit,
) {
    val freqLabels = listOf("60","150","250","500","1K","2K","4K","8K","12K","16K")
    val presets    = MainViewModel.EQ_PRESETS.keys.toList()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 52.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, null, tint = TextPrimaryDark)
            }
            Text("Equalizer", style = MaterialTheme.typography.headlineLarge,
                color = TextPrimaryDark, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(if (eqState.enabled) NebulaViolet.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp,
                        if (eqState.enabled) NebulaViolet.copy(alpha = 0.5f) else DarkBorder,
                        RoundedCornerShape(20.dp))
                    .clickable(onClick = onToggleEq)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (eqState.enabled) "ON" else "OFF",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (eqState.enabled) NebulaViolet else TextTertiaryDark)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Presets
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { preset ->
                val sel = preset == eqState.preset
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                        .background(if (sel) NebulaViolet.copy(alpha = 0.2f) else Color.Transparent)
                        .border(if (sel) 1.dp else 0.5.dp,
                            if (sel) NebulaViolet.copy(alpha = 0.6f) else DarkBorder,
                            RoundedCornerShape(18.dp))
                        .clickable { onPresetChanged(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(preset, style = MaterialTheme.typography.labelMedium,
                        color = if (sel) NebulaViolet else TextSecondaryDark)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // EQ bands — vertical sliders
        Box(modifier = Modifier.weight(1f).alpha(if (eqState.enabled) 1f else 0.4f)) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                eqState.bands.forEachIndexed { i, value ->
                    val barColor = if (i < 5) NebulaCyan else NebulaPink
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)) {
                        Text(if (value != 0f) "${if (value > 0) "+" else ""}${value.toInt()}"
                            else "", style = MaterialTheme.typography.labelSmall,
                            color = barColor, modifier = Modifier.height(16.dp))
                        Slider(
                            value = value, onValueChange = { onBandChanged(i, it) },
                            valueRange = -12f..12f, steps = 23,
                            modifier = Modifier.fillMaxHeight(0.75f)
                                .rotate(270f),
                            colors = SliderDefaults.colors(activeTrackColor = barColor,
                                thumbColor = Color.White, inactiveTrackColor = DarkBorder)
                        )
                        Text(freqLabels[i], style = MaterialTheme.typography.labelSmall,
                            color = TextTertiaryDark, modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Bass Boost
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(NebulaRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.VolumeUp, null, tint = NebulaRed, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bass Boost", style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark)
                        Text("${(eqState.bassBoost * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium, color = NebulaRed)
                    }
                    Slider(value = eqState.bassBoost,
                        onValueChange = { /* handled in VM */ },
                        colors = SliderDefaults.colors(activeTrackColor = NebulaRed,
                            thumbColor = Color.White, inactiveTrackColor = DarkBorder))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
