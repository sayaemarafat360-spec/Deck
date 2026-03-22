package com.sayaem.nebula.ui.screens

import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    songs: List<Song>,
    videos: List<Song>,
    recentSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onVideoClick: (Song) -> Unit,
    onPremiumClick: () -> Unit,
    onStatsClick: () -> Unit,
    onEditTag: ((Song) -> Unit)? = null,
    recentlyAdded: List<Song> = emptyList(),
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {

        // ── Header ─────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Text("Deck", style = MaterialTheme.typography.displaySmall,
                        color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                        .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(13.dp))
                        .clickable(onClick = onStatsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.BarChart, null, tint = NebulaViolet, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Library summary chips ───────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryChip("${songs.size}", "Songs", NebulaViolet, Icons.Filled.MusicNote, Modifier.weight(1f))
                SummaryChip("${videos.size}", "Videos", NebulaRed, Icons.Filled.VideoFile, Modifier.weight(1f))
                SummaryChip("${(songs + videos).groupBy { it.artist }.size}", "Artists", NebulaCyan, Icons.Filled.Person, Modifier.weight(1f))
            }
        }

        // ── Continue Watching (Videos) ──────────────────────────────────
        if (videos.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayCircle, null, tint = NebulaRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Videos", style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                    Text("${videos.size} files", style = MaterialTheme.typography.labelMedium, color = TextTertiaryDark)
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(videos.take(10)) { video ->
                        VideoCard(video = video, onClick = { onVideoClick(video) })
                    }
                }
            }
        }

        // ── Recently Added ──────────────────────────────────────────────
        if (recentlyAdded.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FiberNew, null, tint = NebulaGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recently Added", style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                    Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = TextTertiaryDark)
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentlyAdded.take(10)) { song ->
                        RecentCard(song = song, onClick = { onSongClick(song) })
                    }
                }
            }
        }

        // ── Recently Played ─────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, null, tint = NebulaViolet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Recently Played", style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            val display = if (recentSongs.isEmpty()) songs.take(8) else recentSongs.take(8)
            if (display.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No media yet — scan your storage", style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiaryDark)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(display) { song ->
                        RecentCard(song = song, onClick = { onSongClick(song) })
                    }
                }
            }
        }

        // ── All Songs quick list ─────────────────────────────────────────
        if (songs.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MusicNote, null, tint = NebulaViolet, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Songs", style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                    Text("${songs.size} tracks", style = MaterialTheme.typography.labelMedium, color = TextTertiaryDark)
                }
            }
            items(songs.take(5)) { song ->
                SongRow(song = song, onClick = { onSongClick(song) })
                HorizontalDivider(Modifier.padding(start = 72.dp), color = DarkBorderSubtle, thickness = 0.5.dp)
            }
            if (songs.size > 5) {
                item {
                    Box(
                        Modifier.fillMaxWidth().clickable {}.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("View all ${songs.size} songs in Library →",
                            style = MaterialTheme.typography.labelLarge, color = NebulaViolet)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    value: String, label: String, color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(color.copy(0.1f))
            .border(0.5.dp, color.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall,
            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiaryDark)
    }
}

@Composable
private fun VideoCard(video: Song, onClick: () -> Unit) {
    Box(
        Modifier.width(200.dp).height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A0A0A))
            .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail placeholder
        Box(Modifier.fillMaxSize().background(NebulaRed.copy(0.08f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.VideoFile, null, tint = NebulaRed.copy(0.4f),
                modifier = Modifier.size(40.dp))
        }
        // Play button overlay
        Box(
            Modifier.size(42.dp).clip(CircleShape)
                .background(Color.Black.copy(0.5f))
                .border(1.5.dp, Color.White.copy(0.7f), CircleShape)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White,
                modifier = Modifier.size(22.dp))
        }
        // Duration badge
        Box(
            Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(0.75f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(video.durationFormatted, style = MaterialTheme.typography.labelSmall,
                color = Color.White)
        }
        // Title at bottom
        Box(
            Modifier.fillMaxWidth().align(Alignment.BottomStart)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(video.title, style = MaterialTheme.typography.labelMedium, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentCard(song: Song, onClick: () -> Unit) {
    val colors = listOf(NebulaViolet, NebulaPink, NebulaCyan, NebulaAmber, NebulaGreen)
    val color  = colors[(song.id % colors.size).toInt().let { if (it < 0) -it else it }]
    Column(
        Modifier.width(110.dp).clickable(onClick = onClick)
    ) {
        Box(
            Modifier.size(110.dp).clip(RoundedCornerShape(14.dp))
                .background(color.copy(0.15f))
                .border(0.5.dp, color.copy(0.25f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = color, modifier = Modifier.size(36.dp))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(color)
                    .align(Alignment.BottomEnd).offset((-6).dp, (-6).dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(song.title, style = MaterialTheme.typography.labelMedium, color = TextPrimaryDark,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = TextTertiaryDark,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    val colors = listOf(NebulaViolet, NebulaPink, NebulaCyan, NebulaAmber, NebulaGreen)
    val color  = colors[(song.id % colors.size).toInt().let { if (it < 0) -it else it }]
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(
                onClick  = onClick,
                onLongClick = { onEditTag?.invoke(song) }
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, color = TextPrimaryDark,
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = TextTertiaryDark,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(song.durationFormatted, style = MaterialTheme.typography.labelSmall, color = TextTertiaryDark)
    }
}
