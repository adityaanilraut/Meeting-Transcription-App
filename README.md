# Voice Recording App with AI Transcription & Summary

A production-ready Android voice recording application with real-time transcription using OpenAI Whisper API and AI-powered summary generation using GPT-4. Built with Kotlin, Jetpack Compose, and follows MVVM architecture.

## Features

### 🎙️ Robust Audio Recording
- **Background Recording**: Foreground service that continues recording even when app is in background
- **30-Second Chunks**: Automatically splits recording into 30-second chunks with 2-second overlap for seamless transcription
- **MP3 Format**: High-quality audio compression
- **Real-time Timer**: Live updates showing recording duration

### 🛡️ Edge Case Handling
- **Phone Call Interruption**: Automatically pauses recording when phone call starts, resumes when call ends
- **Audio Focus Management**: Pauses when other apps take audio focus (e.g., music apps)
- **Microphone Source Changes**: Continues recording when Bluetooth/wired headset is connected/disconnected
- **Low Storage Detection**: Checks storage before starting and during recording
- **Process Death Recovery**: Persists recording state in Room database, recovers incomplete sessions on app restart
- **Silent Audio Detection**: Shows warning after 10 seconds of silence (checks microphone is working)

### 📝 AI Transcription
- **OpenAI Whisper API**: Accurate speech-to-text transcription
- **Automatic Processing**: Transcribes chunks as they're recorded
- **Retry Logic**: Exponential backoff with 3 retry attempts
- **Correct Ordering**: Maintains transcript order even if chunks transcribe out of sequence
- **Background Processing**: Uses WorkManager to ensure completion even if app is closed

### 📊 AI Summary Generation
- **GPT-4 Integration**: Generates structured summaries using OpenAI GPT-4o-mini
- **Streaming Updates**: Real-time UI updates as summary is generated
- **Structured Output**: 
  - Meeting title
  - Summary (2-3 paragraphs)
  - Action items (bullet list)
  - Key points (bullet list)
- **Background Processing**: Completes even if app is killed

### 🎨 Modern UI
- **100% Jetpack Compose**: Modern declarative UI
- **Material Design 3**: Beautiful, consistent design
- **Three Screens**:
  - **Dashboard**: List of all recordings with status indicators
  - **Recording**: Large circular record button with live timer
  - **Summary**: Expandable transcript and structured summary view

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow
- **Background Work**: WorkManager
- **Permissions**: Accompanist Permissions
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36

## Setup Instructions

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Voicerecordingapp
```

### 2. Add OpenAI API Key
1. Get your API key from [OpenAI Platform](https://platform.openai.com/api-keys)
2. Open `local.properties` file in the project root
3. Add your API key:
```properties
OPENAI_API_KEY=sk-your-actual-api-key-here
```

### 3. Build and Run
1. Open project in Android Studio
2. Sync Gradle files
3. Connect an Android device or start an emulator
4. Run the app (Shift + F10)

## Permissions Required

The app will request the following permissions:
- **RECORD_AUDIO**: Required for recording audio
- **POST_NOTIFICATIONS** (Android 13+): For showing recording status notifications
- **READ_PHONE_STATE**: To detect phone calls and pause recording

## Architecture Overview

```
app/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAOs
│   │   ├── entity/           # Room entities
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── api/              # Retrofit API interfaces
│   │   └── model/            # API request/response models
│   └── repository/           # Data repositories
├── di/                       # Hilt dependency injection modules
├── service/
│   └── RecordingService.kt  # Foreground recording service
├── ui/
│   ├── navigation/           # Compose navigation
│   ├── screens/              # UI screens
│   ├── theme/                # Material theme
│   └── viewmodel/            # ViewModels
├── util/                     # Utility classes
└── worker/                   # WorkManager workers
```

## How It Works

### Recording Flow
1. User taps record button on Recording screen
2. RecordingService starts as foreground service with notification
3. Audio is recorded in 30-second chunks with 2-second overlap
4. Each chunk is saved to local storage and metadata stored in Room
5. Chunks are automatically queued for transcription via WorkManager

### Transcription Flow
1. TranscriptionWorker picks up pending chunks
2. Audio file is uploaded to OpenAI Whisper API
3. Transcription is saved to Room database with correct order index
4. Retry logic handles failures with exponential backoff
5. Once all chunks are transcribed, meeting status updates to "Processing"

### Summary Flow
1. SummaryViewModel requests summary generation
2. Full transcript is retrieved from Room database (ordered)
3. Transcript sent to OpenAI GPT-4 API with structured prompt
4. Response is streamed back and displayed in real-time
5. Final summary parsed and saved to Room database
6. Meeting status updates to "Completed"

### Edge Case Handling

#### Phone Call Interruption
- PhoneStateListener detects incoming/outgoing calls
- Recording pauses, current chunk is finalized
- Notification shows "Paused - Phone call"
- Resumes automatically when call ends

#### Audio Focus Loss
- AudioFocusChangeListener monitors audio focus
- Pauses when focus lost (e.g., music app starts)
- Notification shows Resume/Stop actions
- Resumes when focus regained

#### Low Storage
- Checks available storage before starting (requires 100MB)
- Monitors during recording (stops if below 50MB)
- Shows clear error message

#### Process Death
- Recording state persisted in Room after each chunk
- On restart, checks for incomplete recording sessions
- Finalizes last chunk and resumes transcription

#### Silent Audio
- Monitors amplitude every second
- Warns user after 10 seconds of silence
- Continues recording (doesn't auto-stop)

## Testing Edge Cases

### Test Phone Call Handling
1. Start recording
2. Call the device from another phone
3. Observe recording pauses with notification
4. End call, observe recording resumes

### Test Audio Focus
1. Start recording
2. Open music app and play music
3. Observe recording pauses
4. Stop music, observe recording can be resumed

### Test Low Storage
1. Fill device storage (or use adb to simulate)
2. Try to start recording
3. Should see "Insufficient storage" message

### Test Process Death
1. Start recording
2. Force stop app from Settings
3. Reopen app
4. Observe chunks are being transcribed in background

### Test Silent Audio
1. Start recording
2. Don't speak for 10+ seconds
3. Observe "No audio detected" warning

## API Costs

- **Whisper API**: ~$0.006 per minute of audio
- **GPT-4o-mini API**: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- For a 30-minute meeting: approximately $0.18 for transcription + $0.01-0.05 for summary

## Future Enhancements

- [ ] Speaker diarization (identify different speakers)
- [ ] Real-time transcription during recording
- [ ] Export summary as PDF or share via email
- [ ] Cloud backup of recordings
- [ ] Support for multiple languages
- [ ] Offline transcription using on-device models
- [ ] Meeting tags and search functionality

## License

[Your License Here]

## Support

For issues or questions, please [create an issue](https://github.com/your-repo/issues) on GitHub.

