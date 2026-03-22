package com.sayaem.nebula.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.sayaem.nebula.data.models.PlaybackState
import com.sayaem.nebula.data.models.RepeatMode
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.ui.components.PlayingIndicator
import com.sayaem.nebula.ui.theme.*

@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    currentSpeed: Float,
    sleepTimerState: SleepTimerState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClose: () -> Unit,
    onEqualizerClick: () -> Unit,
    onSleepTimer: () -> Unit,
    onSpeedClick: () -> Unit,
    onShare: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
) {
    val song = state.currentSong

    val bgAnim by animateColorAsState(
        if (state.isPlaying) NebulaViolet.copy(alpha = 0.25f) else DarkBgSecondary,
        animationSpec = tween(800), label = "bg"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rot"
    )

    var isFav by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(bgAnim, DarkBg)))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // Top bar
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                CircleBtn(Icons.Filled.KeyboardArrowDown, onClick = onClose)
                Text("Now Playing", style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryDark)
                CircleBtn(Icons.Filled.QueueMusic, onClick = {})
            }

            Spacer(Modifier.height(32.dp))

            // Vinyl disc
            Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(290.dp).clip(CircleShape)
                    .background(NebulaViolet.copy(alpha = if (state.isPlaying) 0.12f else 0.04f)))
                Box(
                    modifier = Modifier.size(270.dp).clip(CircleShape)
                        .background(Color(0xFF0A0A14))
                        .rotate(if (state.isPlaying) vinylRotation else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    for (r in listOf(0.95f, 0.8f, 0.65f, 0.5f)) {
                        Box(Modifier.size((270 * r).dp).clip(CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                    }
                    Box(Modifier.size(150.dp).clip(CircleShape)
                        .border(3.dp, NebulaViolet.copy(alpha = 0.2f), CircleShape))
                    Box(Modifier.size(120.dp).clip(CircleShape)
                        .background(NebulaViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center) {
                        if (state.isPlaying) PlayingIndicator(NebulaVioletLight)
                        else Icon(Icons.Filled.MusicNote, null,
                            tint = NebulaViolet, modifier = Modifier.size(44.dp))
                    }
                    Box(Modifier.size(18.dp).clip(CircleShape).background(DarkBg))
                }
            }

            Spacer(Modifier.height(28.dp))

            // Song info
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimaryDark, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(song?.artist ?: "",
                        style = MaterialTheme.typography.bodyLarge, color = TextSecondaryDark,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val favScale by animateFloatAsState(if (isFav) 1.3f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fav")
                IconButton(onClick = {
                    isFav = !isFav
                    song?.let { onToggleFavorite(it) }
                }) {
                    Icon(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null,
                        tint = if (isFav) NebulaPink else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(26.dp).scale(favScale))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Seek bar
            Column {
                Slider(value = state.progress, onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = Color.White,
                        activeTrackColor = NebulaViolet, inactiveTrackColor = DarkBorder))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs(state.position), style = MaterialTheme.typography.labelSmall,
                        color = TextTertiaryDark)
                    Text(formatMs(state.duration), style = MaterialTheme.typography.labelSmall,
                        color = TextTertiaryDark)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Main controls
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Shuffle, null,
                        tint = if (state.isShuffled) NebulaViolet else TextSecondaryDark,
                        modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier = Modifier.size(70.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NebulaViolet, NebulaPink)))
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(state.isPlaying, label = "pp") { playing ->
                        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
                    Icon(
                        when (state.repeatMode) {
                            RepeatMode.ONE  -> Icons.Filled.RepeatOne
                            else            -> Icons.Filled.Repeat
                        }, null,
                        tint = if (state.repeatMode != RepeatMode.NONE) NebulaViolet else TextSecondaryDark,
                        modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Extra actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ExtraBtn(Icons.Filled.Equalizer, "EQ",    onClick = onEqualizerClick)
                ExtraBtn(Icons.Filled.Timer,
                    if (sleepTimerState.isActive) sleepTimerState.remainingFormatted else "Sleep",
                    tint = if (sleepTimerState.isActive) NebulaCyan else null,
                    onClick = onSleepTimer)
                ExtraBtn(Icons.Filled.Speed, "${currentSpeed}x", onClick = onSpeedClick)
                ExtraBtn(Icons.Filled.Share, "Share",
                    onClick = { song?.let { onShare(it) } })
                ExtraBtn(Icons.Filled.Lyrics, "Lyrics", onClick = {})
            }
        }
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ExtraBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint ?: Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiaryDark)
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val m = (ms / 60000).toString().padStart(2, '0')
    val s = ((ms % 60000) / 1000).toString().padStart(2, '0')
    return "$m:$s"
}
