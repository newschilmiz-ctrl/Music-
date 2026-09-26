package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val fileUri: String,
    val posterUri: String? = null,
    val folderName: String = "Music",
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val posterGradientIndex: Int = 0
)

data class Playlist(
    val id: Long,
    val name: String,
    val posterUri: String? = null,
    val posterGradientIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

data class Album(
    val name: String,
    val artist: String,
    val songCount: Int,
    val posterGradientIndex: Int = 0
)

data class Artist(
    val name: String,
    val songCount: Int,
    val posterGradientIndex: Int = 0
)

data class MusicFolder(
    val name: String,
    val path: String,
    val songCount: Int
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class LibraryTab {
    ALL_SONGS,
    RECENTLY_ADDED,
    ALBUMS,
    ARTISTS,
    FOLDERS
}

enum class BottomNavScreen {
    HOME,
    LIBRARY,
    PLAYLISTS,
    PROFILE
}

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

// Preset radiant gradients for album/playlist artwork
object ArtworkPalettes {
    val gradients = listOf(
        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
        listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
        listOf(Color(0xFFF43F5E), Color(0xFFFB923C)),
        listOf(Color(0xFF10B981), Color(0xFF3B82F6)),
        listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
        listOf(Color(0xFFE11D48), Color(0xFF9333EA)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFF0284C7), Color(0xFF0D9488))
    )

    fun getBrush(index: Int): Brush {
        val colors = gradients[index.mod(gradients.size)]
        return Brush.linearGradient(colors)
    }

    fun getPrimaryColor(index: Int): Color {
        return gradients[index.mod(gradients.size)][0]
    }
}
