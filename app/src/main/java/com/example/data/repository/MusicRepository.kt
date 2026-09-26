package com.example.data.repository

import android.content.Context
import com.example.data.local.FavoriteEntity
import com.example.data.local.MusicDao
import com.example.data.local.MusicDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.RecentPlaybackEntity
import com.example.data.local.SongEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.MusicFolder
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val dao: MusicDao = MusicDatabase.getInstance(context).musicDao(),
    private val mediaScanner: MediaScanner = MediaScanner(context)
) {

    private val favoriteIdsFlow: Flow<Set<Long>> = dao.getAllFavoriteIds().map { it.toSet() }

    val allSongs: Flow<List<Song>> = combine(dao.getAllSongs(), favoriteIdsFlow) { entities, favIds ->
        entities.map { it.toDomain(isFavorite = favIds.contains(it.id)) }
    }.flowOn(Dispatchers.Default)

    val recentlyAddedSongs: Flow<List<Song>> = combine(dao.getRecentlyAddedSongs(), favoriteIdsFlow) { entities, favIds ->
        entities.map { it.toDomain(isFavorite = favIds.contains(it.id)) }
    }.flowOn(Dispatchers.Default)

    val favoriteSongs: Flow<List<Song>> = dao.getFavoriteSongs().map { entities ->
        entities.map { it.toDomain(isFavorite = true) }
    }.flowOn(Dispatchers.Default)

    val recentlyPlayedSongs: Flow<List<Song>> = combine(dao.getRecentlyPlayedSongs(), favoriteIdsFlow) { entities, favIds ->
        entities.map { it.toDomain(isFavorite = favIds.contains(it.id)) }
    }.flowOn(Dispatchers.Default)

    val playlists: Flow<List<Playlist>> = dao.getPlaylistsWithCount().map { list ->
        list.map {
            Playlist(
                id = it.id,
                name = it.name,
                posterUri = it.posterUri,
                posterGradientIndex = it.posterGradientIndex,
                createdAt = it.createdAt,
                songCount = it.songCount
            )
        }
    }.flowOn(Dispatchers.Default)

    val albums: Flow<List<Album>> = allSongs.map { songs ->
        songs.groupBy { it.album }
            .map { (albumName, albumSongs) ->
                Album(
                    name = albumName,
                    artist = albumSongs.firstOrNull()?.artist ?: "Various Artists",
                    songCount = albumSongs.size,
                    posterGradientIndex = albumSongs.firstOrNull()?.posterGradientIndex ?: 0
                )
            }
            .sortedBy { it.name }
    }.flowOn(Dispatchers.Default)

    val artists: Flow<List<Artist>> = allSongs.map { songs ->
        songs.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                Artist(
                    name = artistName,
                    songCount = artistSongs.size,
                    posterGradientIndex = artistSongs.firstOrNull()?.posterGradientIndex ?: 0
                )
            }
            .sortedBy { it.name }
    }.flowOn(Dispatchers.Default)

    val folders: Flow<List<MusicFolder>> = allSongs.map { songs ->
        songs.groupBy { it.folderName }
            .map { (folderName, folderSongs) ->
                MusicFolder(
                    name = folderName,
                    path = "/storage/emulated/0/$folderName",
                    songCount = folderSongs.size
                )
            }
            .sortedBy { it.name }
    }.flowOn(Dispatchers.Default)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        combine(dao.getSongsForPlaylist(playlistId), favoriteIdsFlow) { entities, favIds ->
            entities.map { it.toDomain(isFavorite = favIds.contains(it.id)) }
        }.flowOn(Dispatchers.Default)

    fun searchSongs(query: String): Flow<List<Song>> =
        combine(dao.searchSongs(query), favoriteIdsFlow) { entities, favIds ->
            entities.map { it.toDomain(isFavorite = favIds.contains(it.id)) }
        }.flowOn(Dispatchers.Default)

    suspend fun getSongById(songId: Long): Song? = withContext(Dispatchers.IO) {
        val entity = dao.getSongById(songId) ?: return@withContext null
        val isFav = dao.isFavorite(songId)
        entity.toDomain(isFavorite = isFav)
    }

    suspend fun getPlaylistById(playlistId: Long): Playlist? = withContext(Dispatchers.IO) {
        val entity = dao.getPlaylistById(playlistId) ?: return@withContext null
        val songCount = dao.getSongsForPlaylist(playlistId).first().size
        Playlist(
            id = entity.id,
            name = entity.name,
            posterUri = entity.posterUri,
            posterGradientIndex = entity.posterGradientIndex,
            createdAt = entity.createdAt,
            songCount = songCount
        )
    }

    suspend fun scanMusic(forceRescan: Boolean = false) = withContext(Dispatchers.IO) {
        val scanned = mediaScanner.scanDeviceAudio()
        dao.insertSongs(scanned)

        // Seed initial playlists if none exist
        val existingPlaylists = dao.getPlaylistsWithCount().first()
        if (existingPlaylists.isEmpty() && scanned.isNotEmpty()) {
            val p1 = dao.insertPlaylist(
                PlaylistEntity(name = "Morning Study", posterUri = null, posterGradientIndex = 0)
            )
            val p2 = dao.insertPlaylist(
                PlaylistEntity(name = "Chill Vibes", posterUri = null, posterGradientIndex = 1)
            )
            val p3 = dao.insertPlaylist(
                PlaylistEntity(name = "Workout Energy", posterUri = null, posterGradientIndex = 2)
            )

            // Add first few songs to Morning Study and Chill Vibes
            scanned.take(3).forEachIndexed { index, song ->
                dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = p1, songId = song.id, orderIndex = index))
            }
            scanned.drop(2).take(3).forEachIndexed { index, song ->
                dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = p2, songId = song.id, orderIndex = index))
            }
        }
    }

    suspend fun createPlaylist(name: String, posterUri: String?, gradientIndex: Int): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                posterUri = posterUri,
                posterGradientIndex = gradientIndex,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        dao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun updatePlaylistPoster(playlistId: Long, posterUri: String?, gradientIndex: Int) = withContext(Dispatchers.IO) {
        dao.updatePlaylistPoster(playlistId, posterUri, gradientIndex)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        dao.removeSongFromPlaylist(playlistId = playlistId, songId = songId)
    }

    suspend fun getPlaylistsContainingSong(songId: Long): List<Long> = withContext(Dispatchers.IO) {
        dao.getPlaylistsContainingSong(songId)
    }

    suspend fun toggleFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        val isFav = dao.isFavorite(songId)
        if (isFav) {
            dao.removeFavorite(songId)
            false
        } else {
            dao.addFavorite(FavoriteEntity(songId = songId))
            true
        }
    }

    suspend fun recordPlayback(songId: Long, positionMs: Long) = withContext(Dispatchers.IO) {
        dao.recordRecentPlayback(
            RecentPlaybackEntity(
                songId = songId,
                playedAt = System.currentTimeMillis(),
                lastPosition = positionMs
            )
        )
    }

    suspend fun deleteSong(song: Song) = withContext(Dispatchers.IO) {
        dao.deleteSong(song.id)
        try {
            val file = File(song.fileUri)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignored if system permission forbids raw file deletion
        }
    }

    private fun SongEntity.toDomain(isFavorite: Boolean): Song =
        Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            album = this.album,
            duration = this.duration,
            fileUri = this.fileUri,
            posterUri = this.posterUri,
            folderName = this.folderName,
            dateAdded = this.dateAdded,
            isFavorite = isFavorite,
            posterGradientIndex = this.posterGradientIndex
        )
}
