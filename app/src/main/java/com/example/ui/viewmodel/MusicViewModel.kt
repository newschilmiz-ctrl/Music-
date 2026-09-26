package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Album
import com.example.data.model.AppThemeMode
import com.example.data.model.Artist
import com.example.data.model.BottomNavScreen
import com.example.data.model.LibraryTab
import com.example.data.model.MusicFolder
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val playerEngine = AudioPlayerEngine(
        context = application,
        onSongStarted = { song, pos ->
            viewModelScope.launch {
                repository.recordPlayback(song.id, pos)
            }
        }
    )

    val allSongs: StateFlow<List<Song>> = repository.allSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyAdded: StateFlow<List<Song>> = repository.recentlyAddedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val albums: StateFlow<List<Album>> = repository.albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val artists: StateFlow<List<Artist>> = repository.artists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val folders: StateFlow<List<MusicFolder>> = repository.folders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Player state forwards
    val currentSong: StateFlow<Song?> = playerEngine.currentSong
    val isPlaying: StateFlow<Boolean> = playerEngine.isPlaying
    val currentPosition: StateFlow<Long> = playerEngine.currentPosition
    val duration: StateFlow<Long> = playerEngine.duration
    val repeatMode: StateFlow<RepeatMode> = playerEngine.repeatMode
    val isShuffle: StateFlow<Boolean> = playerEngine.isShuffle
    val queue: StateFlow<List<Song>> = playerEngine.queue

    // UI state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _currentBottomNav = MutableStateFlow(BottomNavScreen.HOME)
    val currentBottomNav: StateFlow<BottomNavScreen> = _currentBottomNav.asStateFlow()

    private val _activeLibraryTab = MutableStateFlow(LibraryTab.ALL_SONGS)
    val activeLibraryTab: StateFlow<LibraryTab> = _activeLibraryTab.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistSongs.asStateFlow()

    private val _isFullScreenPlayerOpen = MutableStateFlow(false)
    val isFullScreenPlayerOpen: StateFlow<Boolean> = _isFullScreenPlayerOpen.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    init {
        scanMusic()
        observeSearch()
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(200)
                .distinctUntilChanged()
                .collect { q ->
                    val query = q.trim().lowercase()
                    if (query.isEmpty()) {
                        _searchResults.value = SearchResults()
                    } else {
                        val currentSongs = allSongs.value
                        val currentPlaylists = playlists.value

                        val matchedSongs = currentSongs.filter {
                            it.title.lowercase().contains(query) ||
                            it.artist.lowercase().contains(query) ||
                            it.album.lowercase().contains(query)
                        }

                        val matchedArtists = artists.value.filter {
                            it.name.lowercase().contains(query)
                        }

                        val matchedAlbums = albums.value.filter {
                            it.name.lowercase().contains(query)
                        }

                        val matchedPlaylists = currentPlaylists.filter {
                            it.name.lowercase().contains(query)
                        }

                        _searchResults.value = SearchResults(
                            songs = matchedSongs,
                            artists = matchedArtists,
                            albums = matchedAlbums,
                            playlists = matchedPlaylists
                        )
                    }
                }
        }
    }

    fun scanMusic(force: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanMusic(force)
                showToast("Music library updated")
            } catch (e: Exception) {
                showToast("Scan finished")
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val q = if (queue.isNotEmpty()) queue else allSongs.value
        playerEngine.playSong(song, q)
    }

    fun playAll(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            playerEngine.playSong(songs[0], songs)
        }
    }

    fun shufflePlay(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            if (!isShuffle.value) {
                playerEngine.toggleShuffle()
            }
            playerEngine.playSong(shuffled[0], shuffled)
        }
    }

    fun togglePlayPause() = playerEngine.togglePlayPause()
    fun next() = playerEngine.next()
    fun previous() = playerEngine.previous()
    fun seekTo(posMs: Long) = playerEngine.seekTo(posMs)
    fun toggleShuffle() = playerEngine.toggleShuffle()
    fun toggleRepeat() = playerEngine.toggleRepeat()

    fun addToQueue(song: Song) {
        playerEngine.addToQueue(song)
        showToast("Added to queue: ${song.title}")
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(song.id)
            showToast(if (isNowFav) "Added to Favorites" else "Removed from Favorites")
        }
    }

    fun createPlaylist(name: String, posterUri: String?, gradientIndex: Int, initialSongId: Long? = null) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name, posterUri, gradientIndex)
            if (initialSongId != null) {
                repository.addSongToPlaylist(id, initialSongId)
            }
            showToast("Playlist created: $name")
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
            showToast("Playlist renamed")
            _selectedPlaylist.value?.let {
                if (it.id == playlistId) {
                    _selectedPlaylist.value = it.copy(name = newName)
                }
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            showToast("Playlist deleted")
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song.id)
            showToast("Song added to playlist")
            _selectedPlaylist.value?.let {
                if (it.id == playlistId) {
                    loadPlaylistSongs(playlistId)
                }
            }
        }
    }

    fun addSongToMultiplePlaylists(playlistIds: List<Long>, song: Song) {
        viewModelScope.launch {
            playlistIds.forEach { pid ->
                repository.addSongToPlaylist(pid, song.id)
            }
            showToast("Song added to playlist")
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            showToast("Song removed from playlist")
            loadPlaylistSongs(playlistId)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song)
            showToast("Song deleted from device")
            if (currentSong.value?.id == song.id) {
                next()
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        loadPlaylistSongs(playlist.id)
    }

    fun clearSelectedPlaylist() {
        _selectedPlaylist.value = null
        _selectedPlaylistSongs.value = emptyList()
    }

    private fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            repository.getSongsForPlaylist(playlistId).collect {
                _selectedPlaylistSongs.value = it
            }
        }
    }

    fun setBottomNav(screen: BottomNavScreen) {
        _currentBottomNav.value = screen
        if (screen != BottomNavScreen.PLAYLISTS) {
            _selectedPlaylist.value = null
        }
    }

    fun setLibraryTab(tab: LibraryTab) {
        _activeLibraryTab.value = tab
    }

    fun setFullScreenPlayerOpen(open: Boolean) {
        _isFullScreenPlayerOpen.value = open
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}
