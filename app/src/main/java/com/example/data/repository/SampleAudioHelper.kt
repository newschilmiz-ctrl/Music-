package com.example.data.repository

import android.content.Context
import com.example.data.local.SongEntity
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object SampleAudioHelper {

    data class SampleTrackDef(
        val id: Long,
        val title: String,
        val artist: String,
        val album: String,
        val durationSec: Int,
        val baseFreq: Double,
        val gradientIndex: Int
    )

    val sampleTracks = listOf(
        SampleTrackDef(
            id = 10001L,
            title = "Midnight Horizon",
            artist = "Neon Skyline",
            album = "Cyber Dreams Vol. 1",
            durationSec = 45,
            baseFreq = 220.0, // A3
            gradientIndex = 0
        ),
        SampleTrackDef(
            id = 10002L,
            title = "Ethereal Echoes",
            artist = "Astral Pulse",
            album = "Starlight Melodies",
            durationSec = 52,
            baseFreq = 261.63, // C4
            gradientIndex = 1
        ),
        SampleTrackDef(
            id = 10003L,
            title = "Solar Drift",
            artist = "Aura Bloom",
            album = "Lo-Fi Odyssey",
            durationSec = 40,
            baseFreq = 329.63, // E4
            gradientIndex = 2
        ),
        SampleTrackDef(
            id = 10004L,
            title = "Velvet Velvet",
            artist = "Luna Waves",
            album = "Midnight Cafe",
            durationSec = 60,
            baseFreq = 196.0, // G3
            gradientIndex = 3
        ),
        SampleTrackDef(
            id = 10005L,
            title = "Retrograde Groove",
            artist = "Synth Voyager",
            album = "Arcade Sunset",
            durationSec = 48,
            baseFreq = 293.66, // D4
            gradientIndex = 4
        ),
        SampleTrackDef(
            id = 10006L,
            title = "Golden Hour Chill",
            artist = "Sunny Soul",
            album = "Breeze & Waves",
            durationSec = 50,
            baseFreq = 246.94, // B3
            gradientIndex = 5
        )
    )

    fun ensureSampleAudioFiles(context: Context): List<SongEntity> {
        val samplesDir = File(context.filesDir, "sample_audio")
        if (!samplesDir.exists()) {
            samplesDir.mkdirs()
        }

        return sampleTracks.map { track ->
            val wavFile = File(samplesDir, "track_${track.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 1000) {
                createHarmonicWav(wavFile, track.durationSec, track.baseFreq)
            }

            SongEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration = track.durationSec * 1000L,
                fileUri = wavFile.absolutePath,
                posterUri = null,
                folderName = "Downloads",
                dateAdded = System.currentTimeMillis() - (track.id * 100000L),
                posterGradientIndex = track.gradientIndex
            )
        }
    }

    private fun createHarmonicWav(file: File, durationSeconds: Int, baseFreq: Double) {
        val sampleRate = 22050
        val numSamples = durationSeconds * sampleRate
        val pcmData = ShortArray(numSamples)

        val chords = listOf(1.0, 1.25, 1.5, 1.875) // Major 7th harmonic steps
        val beatSamples = (sampleRate * 0.5).toInt()

        for (i in 0 until numSamples) {
            val chordStep = chords[(i / (sampleRate * 2)) % chords.size]
            val freq = baseFreq * chordStep
            val time = i.toDouble() / sampleRate

            // Envelope for rhythm pulsation
            val beatEnv = 0.6 + 0.4 * sin(2.0 * Math.PI * 2.0 * time)

            // Harmonics
            val sampleVal = (
                sin(2.0 * Math.PI * freq * time) * 0.5 +
                sin(2.0 * Math.PI * (freq * 1.5) * time) * 0.3 +
                sin(2.0 * Math.PI * (freq * 2.0) * time) * 0.2
            ) * beatEnv

            pcmData[i] = (sampleVal * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        FileOutputStream(file).use { fos ->
            val byteCount = numSamples * 2
            val totalDataLen = byteCount + 36

            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size
            header.putShort(1) // AudioFormat 1 = PCM
            header.putShort(1) // NumChannels = 1 (Mono)
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2) // ByteRate
            header.putShort(2) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(byteCount)

            fos.write(header.array())

            val buffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmData) {
                buffer.putShort(s)
            }
            fos.write(buffer.array())
        }
    }
}
