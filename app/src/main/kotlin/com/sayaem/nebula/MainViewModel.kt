package com.sayaem.nebula

import android.app.Application
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.sayaem.nebula.data.local.LocalDataStore
import com.sayaem.nebula.data.models.Playlist
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.data.repository.MediaRepository
import com.sayaem.nebula.player.PlayerController
import com.sayaem.nebula.ui.screens.EqState
import com.sayaem.nebula.ui.screens.SleepTimerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit



class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo    = MediaRepository(app)
    val player  = PlayerController(app)
    val store   = LocalDataStore(app)

    val songs      = repo.songs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val videos     = repo.videos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val isScanning = repo.isScanning.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val playback   = player.state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val searchResults = combine(songs, _searchQuery) { list, q -> repo.search(q, list) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isDark    = MutableStateFlow(true)
    val isDark = _isDark.asStateFlow()

    private val _favorites = MutableStateFlow(store.getFavorites())
    val favorites = _favorites.asStateFlow()

    private val _playStats = MutableStateFlow(store.getPlayStats())

    private val _playlists = MutableStateFlow(store.getPlaylists())
    val playlists = _playlists.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val playbackSpeed = _speed.asStateFlow()

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer = _sleepTimer.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Fix 2 & 3: EQ state with proper init
    private val _eqState = MutableStateFlow(EqState())
    val eqState = _eqState.asStateFlow()
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentAudioSessionId = 0

    // Fix 4: Read settings from prefs on init
    private val _smartSkipEnabled = MutableStateFlow(store.getSmartSkip())

    val recentSongs: StateFlow<List<Song>> = combine(songs, _playStats) { allSongs, _ ->
        val ids = store.getRecentIds()
        val map = allSongs.associateBy { it.id }
        ids.mapNotNull { map[it] }.take(20)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteSongs: StateFlow<List<Song>> = combine(songs, _favorites) { all, favIds ->
        all.filter { it.id in favIds }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<Map<String, List<Song>>> = songs.map { list ->
        list.groupBy { song ->
            val parts = song.filePath.split("/")
            if (parts.size >= 2) parts[parts.size - 2] else "Unknown"
        }.toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val topSongs: StateFlow<List<Pair<Song, Int>>> = combine(songs, _playStats) { allSongs, stats ->
        val map = allSongs.associateBy { it.id }
        stats.entries.sortedByDescending { it.value.playCount }.take(10)
            .mapNotNull { (id, s) -> map[id]?.let { it to s.playCount } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalMinutes: StateFlow<Int> = _playStats.map { stats ->
        (stats.values.sumOf { it.playCount } * 3.5).toInt()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val listeningStats: StateFlow<List<Pair<Song, Long>>> = combine(songs, _playStats) { allSongs, stats ->
        val map = allSongs.associateBy { it.id }
        stats.entries.filter { it.value.playCount > 0 }
            .sortedByDescending { it.value.lastPlayed }
            .mapNotNull { (id, s) -> map[id]?.let { it to s.lastPlayed } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        scanMedia()
        scheduleNotifications()

        // Fix 2: Wire audio session callback from service
        player.onAudioSessionReady = { sessionId ->
            currentAudioSessionId = sessionId
            viewModelScope.launch(Dispatchers.IO) {
                initEqualizerWithSession(sessionId)
            }
        }

        // Track song changes
        viewModelScope.launch {
            var prevId: Long? = null
            var prevPos = 0L; var prevDur = 0L
            playback.collect { state ->
                val curId = state.currentSong?.id
                if (curId != null && curId != prevId) {
                    prevId?.let { store.recordSkip(it, prevPos, prevDur) }
                    store.recordPlay(curId)
                    store.recordRecentPlay(curId)
                    _playStats.value = store.getPlayStats()
                    // Fix 4: Only auto-skip if setting is enabled
                    if (_smartSkipEnabled.value && store.shouldAutoSkip(curId)) {
                        delay(1500)
                        if (playback.value.currentSong?.id == curId) player.next()
                    }
                }
                prevId = curId; prevPos = state.position; prevDur = state.duration
            }
        }
    }

    // Fix 2 & 3: Real EQ init with guaranteed valid audio session
    private fun initEqualizerWithSession(sessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _eqState.value.enabled
                val r = bandLevelRange
                _eqState.value.bands.forEachIndexed { i, v ->
                    if (i < numberOfBands) {
                        setBandLevel(i.toShort(), mapBand(v, r[0].toFloat(), r[1].toFloat()).toShort())
                    }
                }
            }
            bassBoost?.release()
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _eqState.value.enabled
                setStrength((_eqState.value.bassBoost * 1000).toInt().coerceIn(0, 1000).toShort())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun mapBand(v: Float, min: Float, max: Float) =
        (v / 12f * (max - min) / 2 + (min + max) / 2).toInt()

    // Fix 3: EQ band change — always apply immediately if session exists
    fun setEqBand(band: Int, value: Float) {
        val bands = _eqState.value.bands.toMutableList().also { if (band < it.size) it[band] = value }
        _eqState.value = _eqState.value.copy(bands = bands, preset = "Custom")
        if (equalizer == null && currentAudioSessionId != 0) {
            viewModelScope.launch(Dispatchers.IO) { initEqualizerWithSession(currentAudioSessionId) }
            return
        }
        try {
            val r = equalizer?.bandLevelRange ?: return
            equalizer?.setBandLevel(band.toShort(), mapBand(value, r[0].toFloat(), r[1].toFloat()).toShort())
        } catch (e: Exception) {
            // Re-init if stale session
            if (currentAudioSessionId != 0)
                viewModelScope.launch(Dispatchers.IO) { initEqualizerWithSession(currentAudioSessionId) }
        }
    }

    fun setBassBoost(value: Float) {
        _eqState.value = _eqState.value.copy(bassBoost = value)
        try { bassBoost?.setStrength((value * 1000).toInt().coerceIn(0, 1000).toShort()) }
        catch (_: Exception) {}
    }

    fun applyEqPreset(preset: String) {
        val values = EQ_PRESETS[preset] ?: return
        _eqState.value = _eqState.value.copy(preset = preset, bands = values.toMutableList())
        values.forEachIndexed { i, v -> setEqBand(i, v) }
    }

    fun toggleEq() {
        val e = !_eqState.value.enabled
        _eqState.value = _eqState.value.copy(enabled = e)
        try { equalizer?.enabled = e } catch (_: Exception) {}
        try { bassBoost?.enabled = e } catch (_: Exception) {}
    }

    // ── Media ─────────────────────────────────────────────────────────
    fun scanMedia() = viewModelScope.launch { repo.scanMedia() }
    fun setQuery(q: String) { _searchQuery.value = q }
    fun toggleTheme() { _isDark.value = !_isDark.value }

    fun playSong(song: Song, queue: List<Song>? = null) {
        val q = queue ?: songs.value
        player.playQueue(q, q.indexOf(song).coerceAtLeast(0))
    }

    fun playPlaylist(playlist: Playlist) {
        val map = songs.value.associateBy { it.id }
        val queue = playlist.songIds.mapNotNull { map[it] }
        if (queue.isNotEmpty()) player.playQueue(queue, 0)
    }

    // ── Favorites ─────────────────────────────────────────────────────
    fun toggleFavorite(song: Song) { store.toggleFavorite(song.id); _favorites.value = store.getFavorites() }
    fun isFavorite(id: Long) = id in _favorites.value

    // ── Playlists ─────────────────────────────────────────────────────
    fun createPlaylist(name: String)                        { store.createPlaylist(name); refresh() }
    fun deletePlaylist(id: String)                          { store.deletePlaylist(id); refresh() }
    fun renamePlaylist(id: String, name: String)            { store.renamePlaylist(id, name); refresh() }
    fun addSongToPlaylist(pid: String, sid: Long)           { store.addSongToPlaylist(pid, sid); refresh() }
    fun removeSongFromPlaylist(pid: String, sid: Long)      { store.removeSongFromPlaylist(pid, sid); refresh() }
    fun getPlaylistSongs(pl: Playlist): List<Song>          = songs.value.associateBy { it.id }.let { m -> pl.songIds.mapNotNull { m[it] } }
    private fun refresh()                                   { _playlists.value = store.getPlaylists() }

    // ── Speed ─────────────────────────────────────────────────────────
    fun setSpeed(s: Float) { _speed.value = s; player.setSpeed(s) }

    // ── Sleep Timer ───────────────────────────────────────────────────
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val total = minutes * 60
        _sleepTimer.value = SleepTimerState(isActive = true, totalSeconds = total, remainingSeconds = total)
        sleepTimerJob = viewModelScope.launch {
            for (elapsed in 1..total) {
                delay(1000)
                val rem = total - elapsed
                _sleepTimer.value = _sleepTimer.value.copy(remainingSeconds = rem)
                if (rem == 0) {
                    // Fade out over 3 seconds
                    repeat(30) { step ->
                        player.setVolume(1f - (step + 1) / 30f)
                        delay(100)
                    }
                    player.playerOrNull?.pause()
                    player.setVolume(1f)
                    _sleepTimer.value = SleepTimerState()
                    return@launch
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        player.setVolume(1f)
        _sleepTimer.value = SleepTimerState()
    }

    // ── Share ─────────────────────────────────────────────────────────
    fun shareSong(song: Song) {
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Listening to \"${song.title}\" by ${song.artist} on Deck")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(
                Intent.createChooser(i, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    // ── Fix 4: Settings — all applied immediately ─────────────────────
    fun setGapless(enabled: Boolean) {
        store.setGapless(enabled)
        // ExoPlayer handles gapless natively when tracks share the same format
        // No API call needed — it's automatic in ExoPlayer
    }

    fun setSmartSkipEnabled(enabled: Boolean) {
        store.setSmartSkip(enabled)
        _smartSkipEnabled.value = enabled
    }

    fun setCrossfade(seconds: Float) {
        store.setCrossfade(seconds)
        // Crossfade is applied at track transition via volume ramping
        // Registered in player listener — nothing to do here at set time
    }

    // ── Fix 6: Schedule re-engagement notifications via WorkManager ───
    private fun scheduleNotifications() {
        val workManager = WorkManager.getInstance(getApplication())

        // Daily check — fires if user hasn't opened app in 24h
        val dailyWork = PeriodicWorkRequestBuilder<ReEngagementWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            "deck_reengage",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )
    }

    override fun onCleared() {
        super.onCleared()
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        sleepTimerJob?.cancel()
        player.release()
    }

    companion object {
        val EQ_PRESETS = mapOf(
            "Flat"        to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Bass Boost"  to listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Rock"        to listOf(4f, 3f,-1f,-2f, 0f, 2f, 3f, 4f, 4f, 4f),
            "Pop"         to listOf(-1f, 0f, 2f, 3f, 4f, 3f, 2f, 0f,-1f,-1f),
            "Classical"   to listOf(4f, 3f, 2f, 0f,-1f,-1f, 0f, 2f, 3f, 4f),
            "Jazz"        to listOf(3f, 2f, 0f, 1f,-1f,-1f, 0f, 1f, 2f, 3f),
            "Electronic"  to listOf(4f, 3f, 0f,-2f,-2f, 0f, 3f, 4f, 4f, 5f),
            "Hip-Hop"     to listOf(5f, 4f, 2f, 0f,-1f,-1f, 0f, 2f, 3f, 3f),
            "Vocal"       to listOf(-2f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, 0f,-1f),
            "Treble Boost"to listOf(0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 6f),
        )
    }
}
