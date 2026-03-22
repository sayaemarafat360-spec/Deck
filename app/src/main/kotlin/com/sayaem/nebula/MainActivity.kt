package com.sayaem.nebula

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
        setContent { DeckRoot(vm, onExitApp = { finish() }) }
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
fun DeckRoot(vm: MainViewModel, onExitApp: () -> Unit) {
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
    val listeningStats by vm.listeningStats.collectAsStateWithLifecycle()

    var showOnboarding by remember { mutableStateOf(!vm.store.isOnboardingDone()) }
    var screenStack    by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val currentScreen   = screenStack.last()
    var showNowPlaying by remember { mutableStateOf(false) }
    var showEqualizer  by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeed      by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf<Song?>(null) }

    fun navigateTo(screen: Screen) {
        screenStack = if (screen in listOf(Screen.Home, Screen.Library, Screen.Search, Screen.Settings))
            listOf(screen) else screenStack + screen
    }

    fun navigateBack(): Boolean = when {
        showSpeed       -> { showSpeed = false; true }
        showSleepTimer  -> { showSleepTimer = false; true }
        showEqualizer   -> { showEqualizer = false; true }
        showNowPlaying  -> { showNowPlaying = false; true }
        showVideoPlayer != null -> { showVideoPlayer = null; true }
        screenStack.size > 1 -> { screenStack = screenStack.dropLast(1); true }
        else -> false
    }

    val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity
    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (!navigateBack()) onExitApp() }
        }
    }
    DisposableEffect(activity) {
        activity?.onBackPressedDispatcher?.addCallback(backCallback)
        onDispose { backCallback.remove() }
    }

    DeckTheme(darkTheme = isDark) {
        Box(Modifier.fillMaxSize().background(DarkBg)) {

            // Onboarding — shown on first launch only
            if (showOnboarding) {
                OnboardingScreen(onDone = {
                    vm.store.markOnboardingDone()
                    showOnboarding = false
                })
                return@DeckTheme
            }
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Column {
                        AnimatedVisibility(visible = playback.currentSong != null,
                            enter = slideInVertically { it } + fadeIn(),
                            exit  = slideOutVertically { it } + fadeOut()) {
                            MiniPlayer(state = playback,
                                onTogglePlay = { vm.player.togglePlay() },
                                onNext = { vm.player.next() },
                                onExpand = { showNowPlaying = true })
                        }
                        DeckBottomNav(currentScreen) { navigateTo(it) }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            songs = songs, recentSongs = recentSongs,
                            onSongClick    = { vm.playSong(it); showNowPlaying = true },
                            onPremiumClick = { navigateTo(Screen.Premium) },
                            onStatsClick   = { navigateTo(Screen.Stats) },
                        )
                        Screen.Library -> LibraryScreen(
                            songs    = songs, videos = videos,
                            currentSong = playback.currentSong,
                            isPlaying   = playback.isPlaying,
                            favorites   = favorites,
                            playlists   = playlists,
                            folders     = folders,
                            onSongClick  = { vm.playSong(it); showNowPlaying = true },
                            onVideoClick = { song ->
                                showVideoPlayer = song
                                vm.player.player.setMediaItem(
                                    androidx.media3.common.MediaItem.fromUri(song.uri))
                                vm.player.player.prepare()
                                vm.player.player.play()
                            },
                            onPlayPlaylist = { vm.playPlaylist(it); showNowPlaying = true },
                            onCreatePlaylist = { vm.createPlaylist(it) },
                            onDeletePlaylist = { vm.deletePlaylist(it) },
                            onRenamePlaylist = { id, name -> vm.renamePlaylist(id, name) },
                            onAddSongToPlaylist    = { pid, sid -> vm.addSongToPlaylist(pid, sid) },
                            onRemoveSongFromPlaylist = { pid, sid -> vm.removeSongFromPlaylist(pid, sid) },
                        )
                        Screen.Search -> SearchScreen(
                            query = query, onQueryChange = vm::setQuery,
                            results = results, currentSong = playback.currentSong,
                            isPlaying = playback.isPlaying,
                            onSongClick = { vm.playSong(it); showNowPlaying = true }
                        )
                        Screen.Settings -> SettingsScreen(
                            isDark = isDark, onToggleTheme = vm::toggleTheme,
                            onEqualizerClick   = { showEqualizer = true },
                            onPremiumClick     = { navigateTo(Screen.Premium) },
                            onStatsClick       = { navigateTo(Screen.Stats) },
                            onSleepTimerClick  = { showSleepTimer = true },
                            onRescan           = { vm.scanMedia() },
                            onGaplessChanged   = { vm.setGapless(it) },
                            onSmartSkipChanged = { vm.setSmartSkipEnabled(it) },
                            onCrossfadeChanged = { vm.setCrossfade(it) },
                        )
                        Screen.Premium -> PremiumScreen(onBack = { navigateBack() })
                        Screen.Stats   -> StatsScreen(
                            songs = songs, stats = listeningStats,
                            topSongs = topSongs, totalMinutes = totalMin,
                            onBack = { navigateBack() }
                        )
                        else -> HomeScreen(songs = songs, recentSongs = recentSongs,
                            onSongClick = { vm.playSong(it) }, onPremiumClick = {}, onStatsClick = {})
                    }
                }
            }

            // Now Playing
            AnimatedVisibility(visible = showNowPlaying,
                enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NowPlayingScreen(
                    state = playback,
                    currentSpeed    = vm.playbackSpeed.collectAsStateWithLifecycle().value,
                    sleepTimerState = vm.sleepTimer.collectAsStateWithLifecycle().value,
                    onTogglePlay    = { vm.player.togglePlay() },
                    onNext          = { vm.player.next() },
                    onPrev          = { vm.player.previous() },
                    onSeek          = { vm.player.seekToFraction(it) },
                    onToggleShuffle = { vm.player.toggleShuffle() },
                    onCycleRepeat   = { vm.player.cycleRepeat() },
                    onClose         = { showNowPlaying = false },
                    onEqualizerClick = { showEqualizer = true },
                    onSleepTimer    = { showSleepTimer = true },
                    onSpeedClick    = { showSpeed = true },
                    onShare         = { vm.shareSong(it) },
                    onToggleFavorite = { vm.toggleFavorite(it) },
                )
            }

            // Equalizer
            AnimatedVisibility(visible = showEqualizer,
                enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                Box(Modifier.fillMaxSize().background(DarkBg)) {
                    EqualizerScreen(
                        eqState         = vm.eqState.collectAsStateWithLifecycle().value,
                        onBandChanged   = { band, value -> vm.setEqBand(band, value) },
                        onPresetChanged = { vm.applyEqPreset(it) },
                        onToggleEq      = { vm.toggleEq() },
                        onBack          = { showEqualizer = false }
                    )
                }
            }

            // Sleep Timer
            if (showSleepTimer) {
                SleepTimerSheet(
                    state     = vm.sleepTimer.collectAsStateWithLifecycle().value,
                    onStart   = { vm.startSleepTimer(it) },
                    onCancel  = { vm.cancelSleepTimer() },
                    onDismiss = { showSleepTimer = false }
                )
            }

            // Speed Picker
            if (showSpeed) {
                SpeedPickerSheet(
                    current   = vm.playbackSpeed.collectAsStateWithLifecycle().value,
                    onSelect  = { vm.setSpeed(it) },
                    onDismiss = { showSpeed = false }
                )
            }

            // Video Player
            showVideoPlayer?.let { song ->
                VideoPlayerScreen(
                    video  = song,
                    player = vm.player.player,
                    onBack = { showVideoPlayer = null }
                )
            }
        }
    }
}

@Composable
fun DeckBottomNav(current: Screen, onNavigate: (Screen) -> Unit) {
    val tabs = listOf(
        Triple(Screen.Home,     Icons.Filled.Home,         "Home"),
        Triple(Screen.Library,  Icons.Filled.LibraryMusic, "Library"),
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
