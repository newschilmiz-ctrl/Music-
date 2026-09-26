package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun formatDuration_formatsMinutesAndSeconds() {
    val durationMs = 215000L // 3 min 35 sec
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    val formatted = String.format("%d:%02d", minutes, remainingSeconds)
    assertEquals("3:35", formatted)
  }

  @Test
  fun sampleTracks_areValid() {
    val sampleSongs = com.example.data.repository.SampleAudioHelper.sampleSongs
    assertTrue(sampleSongs.isNotEmpty())
    assertEquals(6, sampleSongs.size)
    sampleSongs.forEach { song ->
      assertTrue(song.title.isNotBlank())
      assertTrue(song.artist.isNotBlank())
      assertTrue(song.duration > 0)
    }
  }
}
