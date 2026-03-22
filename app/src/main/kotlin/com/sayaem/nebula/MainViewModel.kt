package com.sayaem.nebula

import android.app.Application
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sayaem.nebula.data.local.LocalDataStore
import com.sayaem.nebula.player.DeckNotificationManager
import com.sayaem.nebula.data.models.Playlist
import com.sayaem.nebula.data.models.Song
import com.sayaem.nebula.data.repository.MediaRepository
import com.sayaem.nebula.player.PlayerController
import com.sayaem.nebula.ui.screens.EqState
import com.sayaem.nebula.ui.screens.SleepTimerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo   = MediaRepository(app)
    val player = PlayerController(app)
    val store  = LocalDataStore(app)
    val notifManager = DeckNotificationManager(app)

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

    private val _eqState = MutableStateFlow(EqState())
    val eqState = _eqState.asStateFlow()
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // ─── Derived flows ────────────────────────────────────────────────
    val recentSongs: StateFlow<List<Song>> = combine(songs, _playStats) { allSongs, _ ->
        val ids     = store.getRecentIds()
        val songMap = allSongs.associateBy { it.id }
        ids.mapNotNull { songMap[it] }.take(20)
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
        val songMap = allSongs.associateBy { it.id }
        stats.entries.sortedByDescending { it.value.playCount }.take(10)
            .mapNotNull { (id, s) -> songMap[id]?.let { it to s.playCount } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalMinutes: StateFlow<Int> = _playStats.map { stats ->
        (stats.values.sumOf { it.playCount } * 3.5).toInt()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val listeningStats: StateFlow<List<Pair<Song, Long>>> = combine(songs, _playStats) { allSongs, stats ->
        val songMap = allSongs.associateBy { it.id }
        stats.entries.filter { it.value.playCount > 0 }
            .sortedByDescending { it.value.lastPlayed }
            .mapNotNull { (id, s) -> songMap[id]?.let { it to s.lastPlayed } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        scanMedia()
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
                    if (store.shouldAutoSkip(curId)) {
                        delay(1500)
                        if (playback.value.currentSong?.id == curId) player.next()
                    }
                    initEqualizer()
                    songs.value.find { it.id == curId }?.let { song ->
                        try { notifManager.showNowPlaying(song, true) } catch (_: Exception) {}
                    }
                }
                prevId = curId; prevPos = state.position; prevDur = state.duration
            }
        }
    }

    fun scanMedia()           = viewModelScope.launch { repo.scanMedia() }
    fun setQuery(q: String)   { _searchQuery.value = q }
    fun toggleTheme()         { _isDark.value = !_isDark.value }

    fun playSong(song: Song, queue: List<Song>? = null) {
        val q = queue ?: songs.value
        player.playQueue(q, q.indexOf(song).coerceAtLeast(0))
    }

    fun playPlaylist(playlist: Playlist) {
        val songMap = songs.value.associateBy { it.id }
        val queue   = playlist.songIds.mapNotNull { songMap[it] }
        if (queue.isNotEmpty()) player.playQueue(queue, 0)
    }

    // ─── Favorites ────────────────────────────────────────────────────
    fun toggleFavorite(song: Song) {
        store.toggleFavorite(song.id)
        _favorites.value = store.getFavorites()
    }
    fun isFavorite(id: Long) = id in _favorites.value

    // ─── Playlists ────────────────────────────────────────────────────
    fun createPlaylist(name: String)                          { store.createPlaylist(name); refresh() }
    fun deletePlaylist(id: String)                            { store.deletePlaylist(id);  refresh() }
    fun renamePlaylist(id: String, name: String)              { store.renamePlaylist(id, name); refresh() }
    fun addSongToPlaylist(playlistId: String, songId: Long)   { store.addSongToPlaylist(playlistId, songId); refresh() }
    fun removeSongFromPlaylist(pid: String, sid: Long)        { store.removeSongFromPlaylist(pid, sid); refresh() }
    fun getPlaylistSongs(pl: Playlist): List<Song>            = songs.value.associateBy { it.id }.let { m -> pl.songIds.mapNotNull { m[it] } }
    private fun refresh()                                     { _playlists.value = store.getPlaylists() }

    // ─── EQ ───────────────────────────────────────────────────────────
    private fun initEqualizer() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            try {
                val sid = player.player.audioSessionId
                if (sid == 0) return@launch
                equalizer?.release()
                equalizer = Equalizer(0, sid).apply {
                    enabled = _eqState.value.enabled
                    _eqState.value.bands.forEachIndexed { i, v ->
                        if (i < numberOfBands) {
                            val r = bandLevelRange
                            setBandLevel(i.toShort(), mapBand(v, r[0].toFloat(), r[1].toFloat()).toShort())
                        }
                    }
                }
                bassBoost?.release()
                bassBoost = BassBoost(0, sid).apply {
                    enabled = _eqState.value.enabled
                    setStrength((_eqState.value.bassBoost * 1000).toInt().toShort())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun mapBand(v: Float, min: Float, max: Float) =
        (v / 12f * (max - min) / 2 + (min + max) / 2).toInt()

    fun setEqBand(band: Int, value: Float) {
        val bands = _eqState.value.bands.toMutableList().also { if (band < it.size) it[band] = value }
        _eqState.value = _eqState.value.copy(bands = bands, preset = "Custom")
        try {
            val r = equalizer?.bandLevelRange ?: return
            equalizer?.setBandLevel(band.toShort(), mapBand(value, r[0].toFloat(), r[1].toFloat()).toShort())
        } catch (_: Exception) {}
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

    // ─── Speed ────────────────────────────────────────────────────────
    fun setSpeed(s: Float) { _speed.value = s; player.setSpeed(s) }

    // ─── Sleep Timer ──────────────────────────────────────────────────
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val total = minutes * 60
        _sleepTimer.value = SleepTimerState(true, total, total)
        sleepTimerJob = viewModelScope.launch {
            for (e in 1..total) {
                delay(1000)
                val rem = total - e
                _sleepTimer.value = _sleepTimer.value.copy(remainingSeconds = rem)
                if (rem == 0) {
                    repeat(10) { step -> player.setVolume(1f - (step+1)/10f); delay(300) }
                    player.player.pause(); player.setVolume(1f)
                    _sleepTimer.value = SleepTimerState()
                    return@launch
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel(); player.setVolume(1f); _sleepTimer.value = SleepTimerState()
    }

    // ─── Share ────────────────────────────────────────────────────────
    fun shareSong(song: Song) {
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "🎵 \"${song.title}\" by ${song.artist} on Deck — The Ultimate Media Player")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(Intent.createChooser(i, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }


    // ─── Settings wiring ──────────────────────────────────────────────
    fun setGapless(enabled: Boolean)        { store.setGapless(enabled) }
    fun setSmartSkipEnabled(enabled: Boolean){ store.setSmartSkip(enabled) }
    fun setCrossfade(seconds: Float)         { store.setCrossfade(seconds) }
    override fun onCleared() {
        super.onCleared()
        equalizer?.release(); bassBoost?.release()
        sleepTimerJob?.cancel(); player.release()
    }

    // ─── Settings wiring ──────────────────────────────────────────────


    companion object {
        val EQ_PRESETS = mapOf(
            "Flat" to listOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f),
            "Bass Boost" to listOf(6f,5f,3f,1f,0f,0f,0f,0f,0f,0f),
            "Rock" to listOf(4f,3f,-1f,-2f,0f,2f,3f,4f,4f,4f),
            "Pop" to listOf(-1f,0f,2f,3f,4f,3f,2f,0f,-1f,-1f),
            "Classical" to listOf(4f,3f,2f,0f,-1f,-1f,0f,2f,3f,4f),
            "Jazz" to listOf(3f,2f,0f,1f,-1f,-1f,0f,1f,2f,3f),
            "Electronic" to listOf(4f,3f,0f,-2f,-2f,0f,3f,4f,4f,5f),
            "Hip-Hop" to listOf(4f,4f,2f,0f,-1f,-1f,0f,2f,3f,3f),
            "Vocal" to listOf(-2f,0f,2f,4f,5f,4f,2f,0f,0f,-1f),
            "Treble" to listOf(0f,0f,0f,0f,0f,2f,4f,5f,6f,6f),
        )
    }
}
