# Implementation Plan: Modern Android Music Player App

Building a feature-rich, high-performance, and visually modern offline Music Player app based on the specified reference design, with local audio scanning, custom playlist creation & management, queue management, full-screen player, mini player, search, navigation drawer, and Room database persistence.

---

## 1. Architecture & Architecture Layers

- **UI Framework:** Jetpack Compose with Material Design 3, custom modern dark/light styling, fluid rounded cards, animated transitions.
- **Audio Engine:** `android.media.MediaPlayer` wrapped in an app-wide `MusicPlayerService` / `PlayerController` with playback states (Playing, Paused, Stopped), track duration, current position, seek, shuffle, repeat (Off, All, One), and queue management.
- **Local Persistence (Room):**
  - `SongEntity`: Cached song metadata (id, title, artist, album, duration, fileUri, posterUri, folderName, dateAdded).
  - `PlaylistEntity`: Custom playlists (id, name, posterUri, createdAt).
  - `PlaylistSongCrossRef`: Many-to-many relationship between playlists and songs with order index.
  - `FavoriteSongEntity`: Quick access to favorite tracks.
  - `RecentPlaybackEntity`: Track history and resume playback positions.
- **Device Media Scanner:** Queries Android `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` to discover all locally stored audio files, album art, and folders with appropriate runtime permissions (`READ_MEDIA_AUDIO` on Android 13+ / `READ_EXTERNAL_STORAGE` on older APIs).
- **Preloaded Sample Music:** Bundled high quality royalty-free sample audio tracks & royalty-free music posters so the app is immediately alive and playable even in an emulator or clean device before manual files are downloaded.

---

## 2. Core Screens & UI Modules

### 2.1 Home Screen (Matching UI Design Reference)
- **Top Bar:**
  - Hamburger Menu button (opens Navigation Drawer)
  - Rounded Search Bar input with search icon & quick filter
  - Settings action button
- **Featured / Trending Poster Carousel:**
  - 3 large horizontal poster cards with album art, song/album title, subtitle, and instant floating play button.
- **Quick Sections:**
  - "Recently Played" horizontal or grid list
  - "Made For You" / "Favorites"
  - "Popular Playlists"
- **Mini Player:** Docked persistent bar right above the bottom navigation with thumbnail, title, artist, play/pause toggle, and skip next.

### 2.2 Navigation Drawer (Side Menu)
- Modern drawer header with app branding & quick stats.
- Items: Home, All Songs, Downloads, Albums, Artists, Playlists, Favorites, Recently Played, Settings, About.

### 2.3 Library Screen
- **Tabs:**
  - *All Songs*: Alphabetical and chronological sorting.
  - *Recently Added*: Newly detected songs.
  - *Albums*: Grid view with album artwork and song counts.
  - *Artists*: Artist list with track counts.
  - *Folders*: File directory navigation for local music folders.
- **Song Item Actions (⋮ menu):**
  - Add to Playlist
  - Add to Queue / Play Next
  - Song Details (File path, format, bitrate, size)
  - Delete from device / Remove from library

### 2.4 Music Player (Full-Screen & Mini Player)
- **Full-Screen Player:**
  - Top bar with collapse arrow, playlist name, and favorite heart toggle.
  - Large rounded square album art poster with elevation & subtle glow.
  - Title, artist name, and album tag.
  - Interactive slider progress bar with current timestamp and total duration.
  - Controls row: Shuffle toggle, Previous, large Play/Pause floating button, Next, Repeat toggle (Off / Loop All / Loop 1).
  - Bottom action bar: Add to Playlist dialog trigger, Queue sheet view, Audio equalizer / speed settings.
- **Mini Player:**
  - Persistent bottom overlay above navigation bar.
  - Tap expands into Full-Screen Player with smooth slide-up animation.

### 2.5 Playlists Management & Details Screen
- **Playlists List:**
  - "+ Create New Playlist" prominent hero action button and card.
  - Grid of playlist poster cards displaying custom artwork, name, song count, play button, and options menu.
- **Create / Edit Playlist Dialog:**
  - Playlist name text field.
  - Visual poster picker (choose from curated music artworks or custom selection).
  - Save and Cancel buttons.
- **Playlist Details Screen:**
  - Header: Large custom playlist poster, title, song count, total runtime, "Play All", "Shuffle Play", and More menu (Rename, Change Poster, Delete).
  - Song list with drag-and-drop or reorder capabilities, remove from playlist, and quick play.
- **Add to Playlist Dialog:**
  - Multi-playlist selector list showing all playlists with checkboxes or instant tap-to-add.
  - In-dialog "+ Create New Playlist" shortcut.
  - Confirmation snackbar: "Song added to playlist".

### 2.6 Search Screen
- Instant live search query across Songs, Artists, Albums, and Playlists.
- Categorized search results chips (All, Songs, Artists, Albums, Playlists).
- Recent searches and search query clear button.

### 2.7 Settings Screen
- **Theme Selection:** Dark, Light, System Default.
- **Audio & Player Settings:** Resume playback on startup, skip silence, sleep timer.
- **Music Scanner:** Manual "Rescan Storage" button with scan progress indicator.
- **Folder Filter:** Select default music folder or ignore short audio clips (e.g., ringtones/voice notes < 30s).
- **About App:** Version info, offline music statement, licenses.

---

## 3. Implementation Steps

1. **Dependencies & App Configuration:**
   - Enable `libs.coil.compose` in `app/build.gradle.kts` for image rendering.
   - Configure unique `applicationId` and descriptive `app_name`.
   - Update `AndroidManifest.xml` with permissions: `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `VIBRATE`, `WAKE_LOCK`.
2. **Room Database & Repositories:**
   - Define entities: `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`, `FavoriteSongEntity`.
   - Define DAOs with reactive `Flow` queries and transaction methods.
   - Build `MusicRepository` integrating Room and Android `MediaStore` scanner.
3. **Audio Playback Engine:**
   - Build `AudioPlayerManager` with `android.media.MediaPlayer`.
   - Implement playlist queue, shuffle algorithm, repeat modes, and seek bar tracking coroutine.
   - Seed sample tracks and preset artwork for immediate out-of-the-box enjoyment.
4. **UI Design & Components:**
   - Theme and colors matching modern dark/neon/music aesthetic.
   - Create reusable components: `SongListItem`, `MusicPosterCard`, `MiniPlayerBar`, `FullScreenPlayerDialog`, `PlaylistCard`.
5. **Screens & Navigation:**
   - Implement `HomeScreen`, `LibraryScreen`, `PlaylistsScreen`, `PlaylistDetailScreen`, `SearchScreen`, `SettingsScreen`, and `NavDrawer`.
   - Bottom navigation bar with badge notifications and active tab states.
6. **Verification & Polish:**
   - Run `compile_applet` to ensure error-free compilation.
   - Verify empty states ("No Music Found" with "Scan Device" button, "No Playlists Yet" with "+ Create New Playlist").
   - Polish animations and touch targets.
