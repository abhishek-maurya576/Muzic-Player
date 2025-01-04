# MuzicPlayer

A modern Android music player application with a clean interface and rich features.

## Features

### Current Features
- 🎵 Local music playback
- 🎮 Media controls (play/pause, next/previous)
- 📱 Notification controls with media info
- ⏳ Progress bar with seek functionality
- 🕒 Real-time duration updates
- 🎧 Background playback
- 📋 Song list with title and artist
- 🔄 Auto-play next song

### Upcoming Features
- [ ] Playlist support
- [ ] Search functionality
- [ ] Song sorting options
- [ ] Equalizer
- [ ] Album art display
- [ ] Shuffle and repeat modes

## Technical Details

### Requirements
- Android Studio Arctic Fox or later
- Minimum SDK: Android 13 (API level 33)
- Kotlin version: 1.8.0

### Architecture
- MVVM Architecture
- Service-based media playback
- Android Media APIs
- Kotlin Coroutines

## Setup
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run on an Android device or emulator

## Permissions
The app requires the following permissions:
- `READ_MEDIA_AUDIO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)
- `FOREGROUND_SERVICE`
- `POST_NOTIFICATIONS`

## Contributing
Feel free to open issues and pull requests for any improvements.

## License
This project is licensed under the MIT License - see the LICENSE file for details.
