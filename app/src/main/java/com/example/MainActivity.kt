package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BottomNavScreen
import com.example.data.model.LibraryTab
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DrawerDestination
import com.example.ui.components.FullScreenPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NavigationDrawerContent
import com.example.ui.components.SongDetailsDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                MusicApp(viewModel = viewModel)
            }
        }
    }
}

enum class ActiveScreen {
    MAIN,
    SEARCH,
    SETTINGS
}

@Composable
fun MusicApp(viewModel: MusicViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val currentBottomNav by viewModel.currentBottomNav.collectAsStateWithLifecycle()
    val activeLibraryTab by viewModel.activeLibraryTab.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistSongs by viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle()
    val isFullScreenPlayerOpen by viewModel.isFullScreenPlayerOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    // Navigation and Dialog state
    var activeScreen by remember { mutableStateOf(ActiveScreen.MAIN) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songForDetails by remember { mutableStateOf<Song?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    // Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.scanMusic(force = true)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    // Handle toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                currentDestination = when (activeScreen) {
                    ActiveScreen.SETTINGS -> DrawerDestination.Settings
                    ActiveScreen.SEARCH -> DrawerDestination.Home
                    ActiveScreen.MAIN -> when (currentBottomNav) {
                        BottomNavScreen.HOME -> DrawerDestination.Home
                        BottomNavScreen.LIBRARY -> when (activeLibraryTab) {
                            LibraryTab.ALL_SONGS -> DrawerDestination.AllSongs
                            LibraryTab.ALBUMS -> DrawerDestination.Albums
                            LibraryTab.ARTISTS -> DrawerDestination.Artists
                            LibraryTab.FOLDERS -> DrawerDestination.Downloads
                            else -> DrawerDestination.AllSongs
                        }
                        BottomNavScreen.PLAYLISTS -> DrawerDestination.Playlists
                        BottomNavScreen.PROFILE -> DrawerDestination.Favorites
                    }
                },
                onNavigate = { destination ->
                    coroutineScope.launch { drawerState.close() }
                    when (destination) {
                        DrawerDestination.Home -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.HOME)
                        }
                        DrawerDestination.AllSongs -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.LIBRARY)
                            viewModel.setLibraryTab(LibraryTab.ALL_SONGS)
                        }
                        DrawerDestination.Downloads -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.LIBRARY)
                            viewModel.setLibraryTab(LibraryTab.FOLDERS)
                        }
                        DrawerDestination.Albums -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.LIBRARY)
                            viewModel.setLibraryTab(LibraryTab.ALBUMS)
                        }
                        DrawerDestination.Artists -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.LIBRARY)
                            viewModel.setLibraryTab(LibraryTab.ARTISTS)
                        }
                        DrawerDestination.Playlists -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.PLAYLISTS)
                        }
                        DrawerDestination.Favorites -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.PROFILE)
                        }
                        DrawerDestination.RecentlyPlayed -> {
                            activeScreen = ActiveScreen.MAIN
                            viewModel.setBottomNav(BottomNavScreen.LIBRARY)
                            viewModel.setLibraryTab(LibraryTab.RECENTLY_ADDED)
                        }
                        DrawerDestination.Settings -> {
                            activeScreen = ActiveScreen.SETTINGS
                        }
                        DrawerDestination.About -> {
                            activeScreen = ActiveScreen.SETTINGS
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (activeScreen == ActiveScreen.MAIN) {
                    Column(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Persistent Mini Player docked above Bottom Navigation when song is loaded
                        if (currentSong != null) {
                            MiniPlayerBar(
                                song = currentSong!!,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                onBarClick = { viewModel.setFullScreenPlayerOpen(true) },
                                onPlayPauseClick = { viewModel.togglePlayPause() },
                                onNextClick = { viewModel.next() }
                            )
                        }

                        // Bottom Navigation: Home, Library, Playlists, Profile
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentBottomNav == BottomNavScreen.HOME,
                                onClick = { viewModel.setBottomNav(BottomNavScreen.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentBottomNav == BottomNavScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = { Text("Home", fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = currentBottomNav == BottomNavScreen.LIBRARY,
                                onClick = { viewModel.setBottomNav(BottomNavScreen.LIBRARY) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentBottomNav == BottomNavScreen.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                        contentDescription = "Library"
                                    )
                                },
                                label = { Text("Library", fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_library")
                            )

                            NavigationBarItem(
                                selected = currentBottomNav == BottomNavScreen.PLAYLISTS,
                                onClick = { viewModel.setBottomNav(BottomNavScreen.PLAYLISTS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentBottomNav == BottomNavScreen.PLAYLISTS) Icons.Filled.QueueMusic else Icons.Outlined.QueueMusic,
                                        contentDescription = "Playlists"
                                    )
                                },
                                label = { Text("Playlists", fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_playlists")
                            )

                            NavigationBarItem(
                                selected = currentBottomNav == BottomNavScreen.PROFILE,
                                onClick = { viewModel.setBottomNav(BottomNavScreen.PROFILE) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentBottomNav == BottomNavScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                },
                                label = { Text("Profile", fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_profile")
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                when (activeScreen) {
                    ActiveScreen.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            searchResults = searchResults,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onBack = { activeScreen = ActiveScreen.MAIN },
                            onSongClick = { song ->
                                viewModel.playSong(song)
                                viewModel.setFullScreenPlayerOpen(true)
                            },
                            onPlaySong = { viewModel.playSong(it) },
                            onPlaylistClick = { playlist ->
                                viewModel.selectPlaylist(playlist)
                                activeScreen = ActiveScreen.MAIN
                                viewModel.setBottomNav(BottomNavScreen.PLAYLISTS)
                            },
                            onAddToPlaylist = { songToAddToPlaylist = it },
                            onAddToQueue = { viewModel.addToQueue(it) },
                            onSongDetails = { songForDetails = it },
                            onDeleteSong = { viewModel.deleteSong(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }

                    ActiveScreen.SETTINGS -> {
                        SettingsScreen(
                            currentTheme = themeMode,
                            onSelectTheme = { viewModel.setThemeMode(it) },
                            isScanning = isScanning,
                            onScanMusic = { viewModel.scanMusic(force = true) },
                            onBack = { activeScreen = ActiveScreen.MAIN }
                        )
                    }

                    ActiveScreen.MAIN -> {
                        when (currentBottomNav) {
                            BottomNavScreen.HOME -> {
                                HomeScreen(
                                    allSongs = allSongs,
                                    recentlyPlayed = recentlyPlayed,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    isScanning = isScanning,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onOpenSearch = { activeScreen = ActiveScreen.SEARCH },
                                    onOpenSettings = { activeScreen = ActiveScreen.SETTINGS },
                                    onSongClick = { song ->
                                        viewModel.playSong(song, allSongs)
                                        viewModel.setFullScreenPlayerOpen(true)
                                    },
                                    onPlaySong = { song ->
                                        if (currentSong?.id == song.id) {
                                            viewModel.togglePlayPause()
                                        } else {
                                            viewModel.playSong(song, allSongs)
                                        }
                                    },
                                    onScanMusic = { viewModel.scanMusic(force = true) },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onSongDetails = { songForDetails = it },
                                    onDeleteSong = { viewModel.deleteSong(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }

                            BottomNavScreen.LIBRARY -> {
                                LibraryScreen(
                                    activeTab = activeLibraryTab,
                                    onTabSelected = { viewModel.setLibraryTab(it) },
                                    allSongs = allSongs,
                                    recentlyAdded = recentlyAdded,
                                    albums = albums,
                                    artists = artists,
                                    folders = folders,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongClick = { song ->
                                        viewModel.playSong(song, allSongs)
                                        viewModel.setFullScreenPlayerOpen(true)
                                    },
                                    onPlaySong = { song ->
                                        if (currentSong?.id == song.id) {
                                            viewModel.togglePlayPause()
                                        } else {
                                            viewModel.playSong(song, allSongs)
                                        }
                                    },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onSongDetails = { songForDetails = it },
                                    onDeleteSong = { viewModel.deleteSong(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onScanMusic = { viewModel.scanMusic(force = true) }
                                )
                            }

                            BottomNavScreen.PLAYLISTS -> {
                                if (selectedPlaylist != null) {
                                    PlaylistDetailScreen(
                                        playlist = selectedPlaylist!!,
                                        songs = selectedPlaylistSongs,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onBack = { viewModel.clearSelectedPlaylist() },
                                        onPlayAll = { viewModel.playAll(selectedPlaylistSongs) },
                                        onShufflePlay = { viewModel.shufflePlay(selectedPlaylistSongs) },
                                        onSongClick = { song ->
                                            viewModel.playSong(song, selectedPlaylistSongs)
                                            viewModel.setFullScreenPlayerOpen(true)
                                        },
                                        onRemoveSong = { song ->
                                            viewModel.removeSongFromPlaylist(selectedPlaylist!!.id, song.id)
                                        },
                                        onAddSongs = {
                                            // Open Library to add songs
                                            viewModel.setBottomNav(BottomNavScreen.LIBRARY)
                                        },
                                        onRenamePlaylist = {
                                            playlistToRename = selectedPlaylist
                                        },
                                        onDeletePlaylist = {
                                            viewModel.deletePlaylist(selectedPlaylist!!.id)
                                        }
                                    )
                                } else {
                                    PlaylistsScreen(
                                        playlists = playlists,
                                        onPlaylistClick = { playlist ->
                                            viewModel.selectPlaylist(playlist)
                                        },
                                        onPlayPlaylist = { playlist ->
                                            viewModel.selectPlaylist(playlist)
                                            coroutineScope.launch {
                                                // Trigger play after selection
                                                if (selectedPlaylistSongs.isNotEmpty()) {
                                                    viewModel.playAll(selectedPlaylistSongs)
                                                }
                                            }
                                        },
                                        onCreateNewPlaylist = { showCreatePlaylistDialog = true },
                                        onRenamePlaylist = { playlist ->
                                            playlistToRename = playlist
                                        },
                                        onDeletePlaylist = { playlist ->
                                            viewModel.deletePlaylist(playlist.id)
                                        }
                                    )
                                }
                            }

                            BottomNavScreen.PROFILE -> {
                                ProfileScreen(
                                    totalSongsCount = allSongs.size,
                                    favoriteSongs = favoriteSongs,
                                    playlistsCount = playlists.size,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongClick = { song ->
                                        viewModel.playSong(song, favoriteSongs)
                                        viewModel.setFullScreenPlayerOpen(true)
                                    },
                                    onPlaySong = { song ->
                                        if (currentSong?.id == song.id) {
                                            viewModel.togglePlayPause()
                                        } else {
                                            viewModel.playSong(song, favoriteSongs)
                                        }
                                    },
                                    onOpenSettings = { activeScreen = ActiveScreen.SETTINGS },
                                    onAddToPlaylist = { songToAddToPlaylist = it },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onSongDetails = { songForDetails = it },
                                    onDeleteSong = { viewModel.deleteSong(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }
                        }
                    }
                }

                // Full-Screen Player Modal Sheet
                if (isFullScreenPlayerOpen && currentSong != null) {
                    FullScreenPlayerSheet(
                        song = currentSong,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        repeatMode = repeatMode,
                        isShuffle = isShuffle,
                        onClose = { viewModel.setFullScreenPlayerOpen(false) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onSeek = { viewModel.seekTo(it) },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
                        onAddToPlaylist = {
                            currentSong?.let { songToAddToPlaylist = it }
                        }
                    )
                }

                // Create Playlist Dialog
                if (showCreatePlaylistDialog) {
                    CreatePlaylistDialog(
                        onDismiss = { showCreatePlaylistDialog = false },
                        onCreate = { name, posterUri, gradientIndex ->
                            viewModel.createPlaylist(name, posterUri, gradientIndex)
                            showCreatePlaylistDialog = false
                        }
                    )
                }

                // Rename Playlist Dialog
                if (playlistToRename != null) {
                    CreatePlaylistDialog(
                        onDismiss = { playlistToRename = null },
                        onCreate = { newName, _, _ ->
                            viewModel.renamePlaylist(playlistToRename!!.id, newName)
                            playlistToRename = null
                        }
                    )
                }

                // Add to Playlist Dialog
                if (songToAddToPlaylist != null) {
                    AddToPlaylistDialog(
                        song = songToAddToPlaylist!!,
                        playlists = playlists,
                        onDismiss = { songToAddToPlaylist = null },
                        onConfirm = { selectedIds ->
                            viewModel.addSongToMultiplePlaylists(selectedIds, songToAddToPlaylist!!)
                            songToAddToPlaylist = null
                        },
                        onCreateNewPlaylist = {
                            showCreatePlaylistDialog = true
                        }
                    )
                }

                // Song Details Dialog
                if (songForDetails != null) {
                    SongDetailsDialog(
                        song = songForDetails!!,
                        onDismiss = { songForDetails = null }
                    )
                }
            }
        }
    }
}
