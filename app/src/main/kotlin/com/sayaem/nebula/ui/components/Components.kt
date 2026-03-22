package com.sayaem.nebula.ui.components

import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.sayaem.nebula.data.models.PlaybackState
import com.sayaem.nebula.ui.theme.*


// ─── Gradient background ──────────────────────────────────────────────
@Composable
fun NebulaBackground(accentColor: Color = NebulaViolet, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        content = content
    )
}

// ─── Section header ───────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, action: String = "", onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall,
            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
        if (action.isNotEmpty()) {
            Text(action, style = MaterialTheme.typography.labelMedium,
                color = NebulaViolet,
                modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

// ─── Song list tile ───────────────────────────────────────────────────
@Composable
fun SongTile(
    title: String,
    artist: String,
    duration: String,
    accentColor: Color = NebulaViolet,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Animated equalizer bars
                PlayingIndicator(color = accentColor)
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = null,
                    tint = accentColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) accentColor else TextPrimaryDark,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, style = MaterialTheme.typography.bodySmall,
                color = TextTertiaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(duration, style = MaterialTheme.typography.labelSmall, color = TextTertiaryDark)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, null, tint = TextTertiaryDark, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Animated playing indicator ───────────────────────────────────────
@Composable
fun PlayingIndicator(color: Color = NebulaViolet) {
    val infiniteTransition = rememberInfiniteTransition(label = "playing")
    val bar1 by infiniteTransition.animateFloat(0.3f, 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(0.7f, 0.2f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(0.5f, 0.9f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "b3")

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.size(20.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { h ->
            Box(Modifier.width(4.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

// ─── Mini Player ──────────────────────────────────────────────────────
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit
) {
    val song = state.currentSong ?: return

    Surface(
        modifier   = Modifier.fillMaxWidth().clickable(onClick = onExpand),
        color      = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Column {
            // Progress bar
            LinearProgressIndicator(
                progress   = { state.progress },
                modifier   = Modifier.fillMaxWidth().height(2.dp),
                color      = NebulaViolet,
                trackColor = DarkBorder
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                        .background(NebulaViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isPlaying) PlayingIndicator()
                    else Icon(Icons.Filled.MusicNote, null, tint = NebulaViolet, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Controls
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(26.dp)
                    )
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(NebulaViolet).clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, null, tint = TextSecondaryDark, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ─── Mood chip ────────────────────────────────────────────────────────
@Composable
fun MoodChip(label: String, icon: @Composable () -> Unit, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.18f) else DarkCard
    val borderColor = if (selected) color.copy(alpha = 0.5f) else DarkBorder

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(23.dp))
            .background(bg)
            .border(0.5.dp, borderColor, RoundedCornerShape(23.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else TextSecondaryDark)
    }
}

// ─── Premium banner ───────────────────────────────────────────────────
@Composable
fun PremiumBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(NebulaViolet, NebulaPink)))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Deck PREMIUM", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(6.dp))
                Text("Unlock EQ, themes\n& ad-free experience",
                    style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("Upgrade", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

// ─── Stat card ────────────────────────────────────────────────────────
@Composable
fun StatCard(value: String, label: String, icon: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        icon()
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium,
            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextTertiaryDark)
    }
}
