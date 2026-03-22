package com.sayaem.nebula

import androidx.compose.ui.*
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.ui.Screen
import com.sayaem.nebula.ui.components.MiniPlayer
import com.sayaem.nebula.ui.screens.*
import com.sayaem.nebula.ui.theme.*

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.scanMedia() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestPermissions()
        setContent { DeckRoot(vm) }
    }

    private fun requestPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
}

@Composable
fun DeckRoot(vm: MainViewModel) {
    val isDark       by vm.isDark.collectAsStateWithLifecycle()
    val songs        by vm.songs.collectAsStateWithLifecycle()
    val videos       by vm.videos.collectAsStateWithLifecycle()
    val playback     by vm.playback.collectAsStateWithLifecycle()
    val query        by vm.searchQuery.collectAsStateWithLifecycle()
    val results      by vm.searchResults.collectAsStateWithLifecycle()
    val favorites    by vm.favoriteSongs.collectAsStateWithLifecycle()
    val playlists    by vm.playlists.collectAsStateWithLifecycle()
    val folders      by vm.folders.collectAsStateWithLifecycle()
    val recentSongs  by vm.recentSongs.collectAsStateWithLifecycle()
    val topSongs     by vm.topSongs.collectAsStateWithLifecycle()
    val totalMin     by vm.totalMinutes.collectAsStateWithLifecycle()
    val listeningStats    by vm.listeningStats.collectAsStateWithLifecycle()
    val recentlyAdded     by vm.recentlyAdded.collectAsStateWithLifecycle()
    val audioSessionId    by vm.audioSessionId.collectAsStateWithLifecycle()

    // Backend state
    val backendUser    by vm.backend.user.collectAsStateWithLifecycle()
    val isPremium      by vm.backend.isPremium.collectAsStateWithLifecycle()
    val prices         by vm.backend.prices.collectAsStateWithLifecycle()
    val backendMsg     by vm.backend.message.collectAsStateWithLifecycle()
    val isSyncing      by vm.backend.isSyncing.collectAsStateWithLifecycle()
    val eqState      by vm.eqState.collectAsStateWithLifecycle()
    val sleepTimer   by vm.sleepTimer.collectAsStateWithLifecycle()
    val speed        by vm.playbackSpeed.collectAsStateWithLifecycle()

    // ── Navigation state ──────────────────────────────────────────────
    var currentTab     by remember { mutableStateOf<Screen>(Screen.Home) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showEqualizer  by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeed      by remember { mutableStateOf(false) }
    var videoSong      by remember { mutableStateOf<Song?>(null) }
    var showOnboarding by remember { mutableStateOf<Boolean>(!vm.store.isOnboardingDone()) }

    // Pull cloud data once on startup if signed in
    LaunchedEffect(backendUser) {
        if (backendUser != null) {
            vm.backend.pullAndMerge(
                onFavorites = { cloudFavs ->
                    // Merge: cloud wins on sign-in, local is already in store
                    cloudFavs.forEach { id -> vm.store.prefs.edit()
                        .putStringSet("fav_synced", cloudFavs.map { it.toString() }.toSet()).apply() }
                },
                onPlaylists = { cloudPlaylists ->
                    // Only import if cloud has more playlists (simple merge strategy)
                    if (cloudPlaylists.size > vm.store.getPlaylists().size) {
                        vm.store.savePlaylists(cloudPlaylists)
                        vm.playlists  // trigger recompose via ViewModel
                    }
                }
            )
        }
    }
    var editingTagSong  by remember { mutableStateOf<com.sayaem.nebula.data.models.Song?>(null) }
    var optionsSong     by remember { mutableStateOf<com.sayaem.nebula.data.models.Song?>(null) }

    // ── Back button — using BackHandler composable (more reliable) ────
    // Order matters: innermost overlay handled first
    if (showSpeed) {
        BackHandler { showSpeed = false }
    }
    if (showSleepTimer) {
        BackHandler { showSleepTimer = false }
    }
    if (showEqualizer) {
        BackHandler { showEqualizer = false }
    }
    if (showNowPlaying) {
        BackHandler { showNowPlaying = false }
    }
    if (videoSong != null) {
        BackHandler { videoSong = null }
    }
    // When on Home tab and nothing is open — do nothing (let system handle = minimize)
    // No BackHandler needed for bottom nav tabs since we don't stack them

    // Show backend messages (sign-in result, premium granted, etc.)
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(backendMsg) {
        backendMsg?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            vm.backend.clearMessage()
        }
    }

    DeckTheme(darkTheme = isDark) {
        Box(Modifier.fillMaxSize().background(DarkBg)) {

            // ── Onboarding ────────────────────────────────────────────
            if (showOnboarding) {
                OnboardingScreen(onDone = {
                    vm.store.markOnboardingDone()
                    showOnboarding = false
                })
                return@DeckTheme
            }

            // ── Main scaffold ─────────────────────────────────────────
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = playback.currentSong != null && videoSong == null,
                            enter   = slideInVertically { it } + fadeIn(),
                            exit    = slideOutVertically { it } + fadeOut()
                        ) {
                            MiniPlayer(
                                state        = playback,
                                onTogglePlay = { vm.player.togglePlay() },
                                onNext       = { vm.player.next() },
                                onExpand     = { showNowPlaying = true }
                            )
                        }
                        if (videoSong == null) {
                            DeckBottomNav(currentTab) { currentTab = it }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (currentTab) {
                        Screen.Home -> HomeScreen(
                            songs       = songs,
                            videos      = videos,
                            recentSongs = recentSongs,
                            onSongClick  = { vm.playSong(it); showNowPlaying = true },
                            onEditTag    = { editingTagSong = it },
                            onMoreClick  = { optionsSong = it },
                            recentlyAdded = recentlyAdded,
                            onVideoClick = { song ->
                                videoSong = song
                                vm.player.playQueue(listOf(song), 0)
                            },
                            onPremiumClick = { currentTab = Screen.Premium },
                            onStatsClick   = { currentTab = Screen.Stats },
                        )
                        Screen.Library -> LibraryScreen(
                            songs    = songs, videos = videos,
                            currentSong = playback.currentSong,
                            isPlaying   = playback.isPlaying,
                            onMoreClick = { optionsSong = it },
                            favorites   = favorites,
                            playlists   = playlists,
                            folders     = folders,
                            onSongClick  = { vm.playSong(it); showNowPlaying = true },
                            onVideoClick = { song ->
                                videoSong = song
                                vm.player.playQueue(listOf(song), 0)
                            },
                            onPlayPlaylist          = { vm.playPlaylist(it); showNowPlaying = true },
                            onCreatePlaylist        = { vm.createPlaylist(it) },
                            onDeletePlaylist        = { vm.deletePlaylist(it) },
                            onRenamePlaylist        = { id, name -> vm.renamePlaylist(id, name) },
                            onAddSongToPlaylist     = { pid, sid -> vm.addSongToPlaylist(pid, sid) },
                            onRemoveSongFromPlaylist = { pid, sid -> vm.removeSongFromPlaylist(pid, sid) },
                        )
                        Screen.Search -> SearchScreen(
                            query = query, onQueryChange = vm::setQuery,
                            results = results, currentSong = playback.currentSong,
                            isPlaying = playback.isPlaying,
                            onSongClick = { vm.playSong(it); showNowPlaying = true }
                        )
                        Screen.Settings -> SettingsScreen(
                            isDark            = isDark,
                            onToggleTheme     = vm::toggleTheme,
                            onEqualizerClick  = { showEqualizer = true },
                            onPremiumClick    = { currentTab = Screen.Premium },
                            onStatsClick      = { currentTab = Screen.Stats },
                            onSleepTimerClick = { showSleepTimer = true },
                            onRescan          = { vm.scanMedia() },
                            onGaplessChanged   = { vm.setGapless(it) },
                            onSmartSkipChanged = { vm.setSmartSkipEnabled(it) },
                            onCrossfadeChanged = { vm.setCrossfade(it) },
                        )
                        Screen.Premium -> PremiumScreen(
                            onBack       = { currentTab = Screen.Home },
                            isPremium    = isPremium,
                            premiumPlan  = vm.backend.premiumPlan.collectAsStateWithLifecycle().value,
                            prices       = prices,
                            onPurchase   = { plan -> vm.backend.grantPremium(plan) },
                        )
                        Screen.Stats   -> StatsScreen(
                            songs = songs, stats = listeningStats,
                            topSongs = topSongs, totalMinutes = totalMin,
                            onBack = { currentTab = Screen.Home }
                        )
                        else -> HomeScreen(
                            songs = songs, videos = videos, recentSongs = recentSongs,
                            onSongClick = { vm.playSong(it) }, onVideoClick = {},
                            onPremiumClick = {}, onStatsClick = {},
                        )
                    }
                }
            }

            // ── Now Playing ───────────────────────────────────────────
            AnimatedVisibility(visible = showNowPlaying,
                enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NowPlayingScreen(
                    state            = playback,
                    currentSpeed     = speed,
                    sleepTimerState  = sleepTimer,
                    isFavorite       = playback.currentSong?.let { vm.isFavorite(it.id) } ?: false,
                    onTogglePlay     = { vm.player.togglePlay() },
                    onNext           = { vm.player.next() },
                    onPrev           = { vm.player.previous() },
                    onSeek           = { vm.player.seekToFraction(it) },
                    onToggleShuffle  = { vm.player.toggleShuffle() },
                    onCycleRepeat    = { vm.player.cycleRepeat() },
                    onClose          = { showNowPlaying = false },
                    onEqualizerClick = { showEqualizer = true },
                    onSleepTimer     = { showSleepTimer = true },
                    onSpeedClick     = { showSpeed = true },
                    onShare          = { vm.shareSong(it) },
                    onToggleFavorite  = { vm.toggleFavorite(it) },
                    onEditTag         = { editingTagSong = it },
                    onQueueSeekTo     = { vm.player.seekToIndex(it) },
                    audioSessionId    = audioSessionId,
                )
            }

            // ── Equalizer ─────────────────────────────────────────────
            AnimatedVisibility(visible = showEqualizer,
                enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                Box(Modifier.fillMaxSize().background(DarkBg)) {
                    EqualizerScreen(
                        eqState         = eqState,
                        onBandChanged   = { band, value -> vm.setEqBand(band, value) },
                        onPresetChanged = { vm.applyEqPreset(it) },
                        onToggleEq      = { vm.toggleEq() },
                        onBack          = { showEqualizer = false }
                    )
                }
            }

            // ── Sleep Timer ───────────────────────────────────────────
            if (showSleepTimer) {
                SleepTimerSheet(
                    state     = sleepTimer,
                    onStart   = { vm.startSleepTimer(it) },
                    onCancel  = { vm.cancelSleepTimer() },
                    onDismiss = { showSleepTimer = false }
                )
            }

            // ── Speed ─────────────────────────────────────────────────
            if (showSpeed) {
                SpeedPickerSheet(
                    current   = speed,
                    onSelect  = { vm.setSpeed(it) },
                    onDismiss = { showSpeed = false }
                )
            }

            // ── Song Options Sheet (3-dot) ────────────────────────────
            optionsSong?.let { song ->
                val favs by vm.favorites.collectAsStateWithLifecycle()
                SongOptionsSheet(
                    song                   = song,
                    isFavorite             = song.id in favs,
                    playlists              = playlists,
                    onDismiss              = { optionsSong = null },
                    onPlayNow              = { vm.playSong(song); showNowPlaying = true },
                    onPlayNext             = { vm.playNext(song) },
                    onAddToQueue           = { vm.addToQueue(song) },
                    onAddToPlaylist        = { pid -> vm.addSongToPlaylist(pid, song.id) },
                    onCreateAndAddPlaylist = { name -> vm.createPlaylistAndAddSong(name, song) },
                    onToggleFavorite       = { vm.toggleFavorite(song) },
                    onEditTags             = { editingTagSong = song; optionsSong = null },
                    onShare                = { vm.shareSong(song) },
                    onDelete               = { vm.deleteSong(song) {} },
                )
            }

            // ── Tag Editor ───────────────────────────────────────────
            editingTagSong?.let { song ->
                TagEditorScreen(
                    song   = song,
                    onSave = { t, ar, al ->
                        vm.updateTags(song, t, ar, al) { editingTagSong = null }
                    },
                    onBack = { editingTagSong = null }
                )
            }

            // ── Video Player ──────────────────────────────────────────
            AnimatedVisibility(visible = videoSong != null,
                enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                videoSong?.let { song ->
                    VideoPlayerScreen(
                        video  = song,
                        player = vm.player.playerOrNull,
                        onBack = { videoSong = null }
                    )
                }
            }
        }
    }
}

@Composable
fun DeckBottomNav(current: Screen, onNavigate: (Screen) -> Unit) {
    val tabs = listOf(
        Triple(Screen.Home,     Icons.Filled.Home,         "Home"),
        Triple(Screen.Library,  Icons.Filled.VideoLibrary, "Library"),
        Triple(Screen.Search,   Icons.Filled.Search,       "Search"),
        Triple(Screen.Settings, Icons.Filled.Settings,     "Settings"),
    )
    NavigationBar(containerColor = DarkBgSecondary, tonalElevation = 0.dp) {
        tabs.forEach { (screen, icon, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick  = { onNavigate(screen) },
                icon     = { Icon(icon, label, modifier = Modifier.size(22.dp)) },
                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = NebulaViolet,
                    selectedTextColor   = NebulaViolet,
                    unselectedIconColor = TextTertiaryDark,
                    unselectedTextColor = TextTertiaryDark,
                    indicatorColor      = NebulaViolet.copy(alpha = 0.15f),
                )
            )
        }
    }
}
