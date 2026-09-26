package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerEngine(
    private val context: Context,
    private val onSongStarted: (Song, Long) -> Unit = { _, _ -> }
) {
    private val TAG = "AudioPlayerEngine"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private var currentIndex: Int = -1

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                handleTrackCompletion()
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                _isPlaying.value = false
                true
            }
        }
    }

    fun playSong(song: Song, newQueue: List<Song> = emptyList()) {
        if (newQueue.isNotEmpty()) {
            _queue.value = newQueue
            currentIndex = newQueue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        } else if (_queue.value.none { it.id == song.id }) {
            _queue.value = listOf(song)
            currentIndex = 0
        } else {
            currentIndex = _queue.value.indexOfFirst { it.id == song.id }
        }

        startPlaying(song)
    }

    private fun startPlaying(song: Song) {
        _currentSong.value = song
        _currentPosition.value = 0L

        try {
            if (mediaPlayer == null) {
                initMediaPlayer()
            }

            mediaPlayer?.reset()

            val uriStr = song.fileUri
            if (uriStr.startsWith("content://")) {
                mediaPlayer?.setDataSource(context, Uri.parse(uriStr))
            } else {
                val file = File(uriStr)
                if (file.exists()) {
                    mediaPlayer?.setDataSource(file.absolutePath)
                } else {
                    mediaPlayer?.setDataSource(context, Uri.parse(uriStr))
                }
            }

            mediaPlayer?.prepare()
            val trackDuration = mediaPlayer?.duration?.toLong() ?: song.duration
            _duration.value = if (trackDuration > 0) trackDuration else song.duration

            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressUpdates()
            onSongStarted(song, 0L)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback for ${song.title}: ${e.message}")
            _isPlaying.value = false
            // Fallback duration
            _duration.value = song.duration
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (_currentSong.value == null) {
            if (_queue.value.isNotEmpty()) {
                playSong(_queue.value[0], _queue.value)
            }
            return
        }

        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            if (!player.isPlaying) {
                player.start()
                _isPlaying.value = true
                startProgressUpdates()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing: ${e.message}")
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}")
        }
    }

    fun next() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        if (_repeatMode.value == RepeatMode.ONE && _currentSong.value != null) {
            startPlaying(_currentSong.value!!)
            return
        }

        if (_isShuffle.value && currentQ.size > 1) {
            var nextIdx: Int
            do {
                nextIdx = (currentQ.indices).random()
            } while (nextIdx == currentIndex && currentQ.size > 1)
            currentIndex = nextIdx
        } else {
            currentIndex++
            if (currentIndex >= currentQ.size) {
                if (_repeatMode.value == RepeatMode.ALL) {
                    currentIndex = 0
                } else {
                    pause()
                    seekTo(0L)
                    return
                }
            }
        }

        val nextSong = currentQ[currentIndex]
        startPlaying(nextSong)
    }

    fun previous() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (_currentPosition.value > 3000L) {
            seekTo(0L)
            return
        }

        if (_isShuffle.value && currentQ.size > 1) {
            currentIndex = (currentQ.indices).random()
        } else {
            currentIndex--
            if (currentIndex < 0) {
                currentIndex = if (_repeatMode.value == RepeatMode.ALL) currentQ.size - 1 else 0
            }
        }

        val prevSong = currentQ[currentIndex]
        startPlaying(prevSong)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun addToQueue(song: Song) {
        val current = _queue.value.toMutableList()
        current.add(song)
        _queue.value = current
    }

    fun clearQueue() {
        _queue.value = emptyList()
        currentIndex = -1
        pause()
        _currentSong.value = null
        _currentPosition.value = 0L
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentSong.value?.let { startPlaying(it) }
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (currentIndex < _queue.value.size - 1) {
                    next()
                } else {
                    _isPlaying.value = false
                    stopProgressUpdates()
                    seekTo(0L)
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition.toLong()
                        }
                    }
                } catch (e: Exception) {
                    // Ignored during state transition
                }
                delay(250)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        }
        mediaPlayer = null
    }
}
