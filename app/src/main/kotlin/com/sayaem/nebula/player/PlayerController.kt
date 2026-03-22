package com.sayaem.nebula.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sayaem.nebula.data.models.PlaybackState
import com.sayaem.nebula.data.models.RepeatMode
import com.sayaem.nebula.data.models.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerController(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) { syncState() }
            override fun onIsPlayingChanged(isPlaying: Boolean) { syncState(); if (isPlaying) startPositionUpdates() else stopPositionUpdates() }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) { syncState() }
        })
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        player.clearMediaItems()
        songs.forEach { song ->
            player.addMediaItem(MediaItem.Builder().setUri(song.uri).setMediaId(song.id.toString()).build())
        }
        _state.value = _state.value.copy(queue = songs, queueIndex = startIndex, currentSong = songs.getOrNull(startIndex))
        player.seekToDefaultPosition(startIndex)
        player.prepare()
        player.play()
    }

    fun playSong(song: Song) = playQueue(listOf(song))

    fun togglePlay() { if (player.isPlaying) player.pause() else player.play() }
    fun next()       { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun previous()   { if (player.currentPosition > 3000) player.seekTo(0) else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun seekToFraction(fraction: Float) = player.seekTo((player.duration * fraction).toLong())

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.value = _state.value.copy()
    }

    fun setVolume(v: Float) { player.volume = v.coerceIn(0f, 1f) }

    fun toggleShuffle() {
        val shuffled = !_state.value.isShuffled
        player.shuffleModeEnabled = shuffled
        _state.value = _state.value.copy(isShuffled = shuffled)
    }

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL  -> RepeatMode.ONE
            RepeatMode.ONE  -> RepeatMode.NONE
        }
        player.repeatMode = when (next) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL  -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE  -> Player.REPEAT_MODE_ONE
        }
        _state.value = _state.value.copy(repeatMode = next)
    }

    private fun syncState() {
        val idx  = player.currentMediaItemIndex
        val song = _state.value.queue.getOrNull(idx)
        _state.value = _state.value.copy(
            currentSong = song,
            isPlaying   = player.isPlaying,
            position    = player.currentPosition.coerceAtLeast(0),
            duration    = player.duration.coerceAtLeast(0),
            queueIndex  = idx,
        )
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _state.value = _state.value.copy(
                    position = player.currentPosition.coerceAtLeast(0),
                    duration = player.duration.coerceAtLeast(0),
                )
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() { positionJob?.cancel() }

    fun release() {
        scope.cancel()
        player.release()
    }
}
