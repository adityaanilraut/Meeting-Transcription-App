# Quick Start Guide

This guide will help you get the Voice Recording App up and running in minutes.

## Prerequisites

- Android Studio (latest version recommended)
- Android device or emulator (API 24+)
- OpenAI API key ([Get one here](https://platform.openai.com/api-keys))

## Setup Steps

### 1. Configure API Key (REQUIRED)

1. Open `local.properties` in the project root
2. Replace `your_openai_api_key_here` with your actual OpenAI API key:
   ```properties
   OPENAI_API_KEY=sk-proj-your-actual-key-here
   ```
3. Save the file

**Important**: The app will not work without a valid API key!

### 2. Sync and Build

1. Open the project in Android Studio
2. Wait for Gradle sync to complete (this may take a few minutes on first run)
3. If you see any sync errors, try:
   - File → Invalidate Caches → Invalidate and Restart
   - Clean Project (Build → Clean Project)
   - Rebuild Project (Build → Rebuild Project)

### 3. Run the App

1. Connect your Android device via USB (with USB debugging enabled) OR start an Android emulator
2. Click the "Run" button (green play icon) or press Shift + F10
3. Select your device/emulator
4. Wait for the app to install and launch

## First Recording

### Basic Recording Flow

1. **Grant Permissions**: On first launch, tap "New Recording" (+ button)
2. Grant microphone permission when prompted
3. **Start Recording**: Tap the large circular button
4. **Speak**: The timer will show elapsed time
5. **Stop Recording**: Tap the button again to stop
6. **View Summary**: You'll be taken back to the dashboard

### View Your Recording

1. On the dashboard, tap on your recording
2. You'll see:
   - **Title** (editable): Default shows date/time
   - **Summary** section: Will show "Transcribing audio..." initially
   - **Transcript** section: Tap to expand and see full transcript

### Wait for Processing

- **Transcription**: Takes ~10-30 seconds for a 30-second recording
- **Summary**: Appears automatically after transcription completes
- **Background**: Processing continues even if you close the app!

## Testing Edge Cases

### Test 1: Phone Call Interruption
```
1. Start recording
2. Call your device from another phone
3. Notice: Recording pauses with notification "Paused - Phone call"
4. End the call
5. Notice: Recording automatically resumes
```

### Test 2: Audio Focus Loss
```
1. Start recording
2. Open Spotify/YouTube Music and play a song
3. Notice: Recording pauses with notification
4. Tap "Resume" in notification or stop the music
5. Recording continues
```

### Test 3: Silent Audio Detection
```
1. Start recording
2. Stay silent for 10 seconds
3. Notice: Status message shows "No audio detected - Check microphone"
4. Speak again
5. Warning disappears
```

### Test 4: Background Recording
```
1. Start recording
2. Press home button to minimize app
3. Notice: Notification shows recording is active
4. Open other apps, use your phone normally
5. Recording continues in background
6. Pull down notification shade and tap "Stop" when done
```

### Test 5: Process Recovery
```
1. Start recording
2. Go to Settings → Apps → Voice Recording App
3. Force stop the app
4. Reopen the app
5. Check dashboard - recording should be there with "Processing" status
6. Transcription continues in background
```

## Troubleshooting

### Problem: "Insufficient storage" error
**Solution**: Free up at least 100MB of space on your device

### Problem: Recording not transcribing
**Solution**: 
- Check internet connection (WiFi or mobile data)
- Verify API key is correct in `local.properties`
- Check OpenAI API key has credits available

### Problem: App crashes on launch
**Solution**:
- Rebuild project (Build → Rebuild Project)
- Check Logcat for error messages
- Ensure all dependencies downloaded correctly

### Problem: Permissions not being requested
**Solution**:
- Clear app data: Settings → Apps → Voice Recording App → Storage → Clear Data
- Uninstall and reinstall the app

### Problem: Summary not generating
**Solution**:
- Wait longer - summary generates after ALL chunks are transcribed
- Check Logcat for API errors
- Verify transcript exists (tap to expand transcript section)

## Development Tips

### View Logs
```
1. Open Logcat in Android Studio
2. Filter by "RecordingService" or "TranscriptionRepository"
3. Watch real-time logging of recording/transcription
```

### Debug Recording
```kotlin
// Key tags to filter in Logcat:
RecordingService - Recording state changes
TranscriptionRepository - Transcription progress
SummaryRepository - Summary generation
TranscriptionWorker - Background transcription
```

### Check Database
```
1. Device File Explorer in Android Studio
2. Navigate to: data/data/com.example.voicerecordingapp/databases/
3. Download voice_recording_db
4. Use DB Browser for SQLite to inspect
```

### Monitor Storage
```
adb shell df /data
```

## Next Steps

1. Try recording a longer meeting (5-10 minutes)
2. Edit the meeting title
3. Share the summary with colleagues
4. Test all edge cases to see robust handling

## Need Help?

- Check `README.md` for detailed architecture information
- Review code comments for implementation details
- Check GitHub issues for known problems

## API Usage Monitoring

Monitor your OpenAI API usage at:
https://platform.openai.com/usage

Typical costs:
- 1 minute recording: ~$0.006 transcription + ~$0.001 summary
- 30 minute meeting: ~$0.18 transcription + ~$0.05 summary

