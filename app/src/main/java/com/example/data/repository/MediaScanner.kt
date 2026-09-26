package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScanner(private val context: Context) {

    suspend fun scanDeviceAudio(): List<SongEntity> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<SongEntity>()

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Ignore audio clips shorter than 10 seconds (ringtones, notifications)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                var index = 0
                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val title = c.getString(titleColumn) ?: "Unknown Track"
                    val artist = c.getString(artistColumn).let {
                        if (it == null || it == "<unknown>") "Unknown Artist" else it
                    }
                    val album = c.getString(albumColumn) ?: "Unknown Album"
                    val duration = c.getLong(durationColumn)
                    val data = if (dataColumn >= 0) c.getString(dataColumn) else null
                    val dateAdded = c.getLong(dateAddedColumn) * 1000L

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val fileUri = data ?: contentUri

                    val folderName = if (data != null) {
                        try {
                            File(data).parentFile?.name ?: "Music"
                        } catch (e: Exception) {
                            "Music"
                        }
                    } else "Music"

                    songList.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            fileUri = fileUri,
                            posterUri = null,
                            folderName = folderName,
                            dateAdded = dateAdded,
                            posterGradientIndex = (index++) % 8
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Permission not granted or query failed
        }

        // Always merge with sample tracks to guarantee immediately playable music in emulator/fresh install
        val sampleTracks = SampleAudioHelper.ensureSampleAudioFiles(context)
        val combined = (sampleTracks + songList).distinctBy { it.id }
        combined
    }
}
