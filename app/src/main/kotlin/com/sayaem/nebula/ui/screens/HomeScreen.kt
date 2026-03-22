package com.sayaem.nebula.ui.screens

import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import com.sayaem.nebula.ui.components.*
import com.sayaem.nebula.ui.theme.*
import java.util.Calendar


@Composable
fun HomeScreen(
    recentSongs: List<com.sayaem.nebula.data.models.Song> = emptyList(),
    songs: List<com.sayaem.nebula.data.models.Song>,
    onSongClick: (com.sayaem.nebula.data.models.Song) -> Unit,
    onPremiumClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            else      -> "Good evening"
        }
    }
    var selectedMood by remember { mutableStateOf(0) }
    val moods = listOf("All", "Energetic", "Happy", "Calm", "Focus")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // ── Greeting header ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark)
                    Text("What will you play?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeaderIconButton(Icons.Filled.Notifications)
                    HeaderIconButton(Icons.Filled.AccountCircle)
                }
            }
        }

        // ── Mood chips ───────────────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val moodIcons = listOf(Icons.Filled.MusicNote, Icons.Filled.Bolt,
                    Icons.Filled.SentimentVerySatisfied, Icons.Filled.Cloud, Icons.Filled.Psychology)
                val moodColors = listOf(NebulaViolet, NebulaRed, NebulaAmber, NebulaCyan, NebulaGreen)
                itemsIndexed(moods) { i, mood ->
                    MoodChip(
                        label = mood,
                        icon = { Icon(moodIcons[i], null,
                            tint = if (selectedMood == i) moodColors[i] else TextTertiaryDark,
                            modifier = Modifier.size(15.dp)) },
                        selected = selectedMood == i,
                        color = moodColors[i],
                        onClick = { selectedMood = i }
                    )
                }
            }
        }

        // ── Recently played ──────────────────────────────────────────
        item { SectionHeader("Recently Played", "See all") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                val recentSongs = if (recentSongs.isEmpty()) songs.takeLast(8).reversed() else recentSongs.take(8)
                if (recentSongs.isEmpty()) {
                    item { EmptyRecentCard() }
                } else {
                    items(recentSongs) { song ->
                        RecentSongCard(song = song, onClick = { onSongClick(song) })
                    }
                }
            }
        }

        // ── Quick picks grid ─────────────────────────────────────────
        item { SectionHeader("Quick Picks") }
        item {
            val quickItems = listOf(
                Triple("Liked Songs",    Icons.Filled.Favorite,    NebulaPink),
                Triple("Recently Added", Icons.Filled.AutoAwesome,  NebulaViolet),
                Triple("Top Mix",        Icons.Filled.Album,        NebulaAmber),
                Triple("Chill Vibes",    Icons.Filled.Cloud,        NebulaCyan),
                Triple("Focus Mode",     Icons.Filled.Psychology,   NebulaGreen),
                Triple("Night Drive",    Icons.Filled.NightlightRound, NebulaVioletLight),
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                quickItems.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (label, icon, color) ->
                            QuickPickCard(label, icon, color, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Stats preview ────────────────────────────────────────────
        item { Spacer(Modifier.height(20.dp)) }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkCard)
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onStatsClick)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BarChart, null,
                                tint = NebulaViolet, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Your Week", style = MaterialTheme.typography.labelMedium,
                                color = NebulaViolet)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${songs.size} songs in library",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Text("Tap to see your stats",
                            style = MaterialTheme.typography.bodySmall, color = TextTertiaryDark)
                    }
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(NebulaViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DonutLarge, null,
                            tint = NebulaViolet, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // ── Premium banner ───────────────────────────────────────────
        item { Spacer(Modifier.height(16.dp)) }
        item { PremiumBanner(onClick = onPremiumClick) }
    }
}

@Composable
private fun HeaderIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
            .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = TextSecondaryDark, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RecentSongCard(song: com.sayaem.nebula.data.models.Song, onClick: () -> Unit) {
    val colors = listOf(NebulaViolet, NebulaPink, NebulaCyan, NebulaAmber, NebulaGreen)
    val color = colors[((song.id % colors.size).toInt() + colors.size) % colors.size]

    Column(
        modifier = Modifier.width(125.dp).clip(RoundedCornerShape(16.dp))
            .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(110.dp)
                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .size(28.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(song.title, style = MaterialTheme.typography.labelMedium,
                color = TextPrimaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(song.artist, style = MaterialTheme.typography.bodySmall,
                color = TextTertiaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyRecentCard() {
    Box(
        modifier = Modifier.width(200.dp).height(130.dp).clip(RoundedCornerShape(16.dp))
            .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.LibraryMusic, null, tint = TextTertiaryDark, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("No songs yet", style = MaterialTheme.typography.bodySmall, color = TextTertiaryDark)
        }
    }
}

@Composable
private fun QuickPickCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                          color: Color, modifier: Modifier) {
    Row(
        modifier = modifier.height(52.dp).clip(RoundedCornerShape(14.dp))
            .background(DarkCard).border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(52.dp).fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = TextPrimaryDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
