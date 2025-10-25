package com.example.voicerecordingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicerecordingapp.ui.viewmodel.PlaybackViewModel
import com.example.voicerecordingapp.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(meetingId) {
        viewModel.loadMeeting(meetingId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.meeting?.title ?: "Playback",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                PlaybackLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            uiState.errorMessage != null -> {
                PlaybackErrorState(
                    message = uiState.errorMessage ?: "Unknown error",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            uiState.chunks.isEmpty() -> {
                PlaybackEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                PlaybackContent(
                    uiState = uiState,
                    onPlay = { viewModel.play() },
                    onPause = { viewModel.pause() },
                    onStop = { viewModel.stop() },
                    onSeek = { position -> viewModel.seekTo(position) },
                    onPreviousChunk = { viewModel.previousChunk() },
                    onNextChunk = { viewModel.nextChunk() },
                    onLoadChunk = { index -> viewModel.loadChunk(index) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun PlaybackLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading recording...")
        }
    }
}

@Composable
fun PlaybackErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaybackEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "No audio",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No audio chunks found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaybackContent(
    uiState: com.example.voicerecordingapp.ui.viewmodel.PlaybackUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onPreviousChunk: () -> Unit,
    onNextChunk: () -> Unit,
    onLoadChunk: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Meeting info
        MeetingInfo(
            meeting = uiState.meeting,
            totalDuration = formatTotalDuration(uiState.chunks),
            currentChunkInfo = formatCurrentChunkInfo(uiState.currentChunkIndex, uiState.chunks.size)
        )
        
        // Progress section
        ProgressSection(
            currentPosition = uiState.currentPosition,
            duration = uiState.duration,
            isPrepared = uiState.isPrepared,
            onSeek = onSeek
        )
        
        // Playback controls
        PlaybackControls(
            isPlaying = uiState.isPlaying,
            isPrepared = uiState.isPrepared,
            canGoPrevious = uiState.currentChunkIndex > 0,
            canGoNext = uiState.currentChunkIndex < uiState.chunks.size - 1,
            onPlay = onPlay,
            onPause = onPause,
            onStop = onStop,
            onPrevious = onPreviousChunk,
            onNext = onNextChunk
        )
        
        // Chunk navigation
        if (uiState.chunks.size > 1) {
            ChunkNavigation(
                chunks = uiState.chunks,
                currentChunkIndex = uiState.currentChunkIndex,
                onChunkSelected = onLoadChunk
            )
        }
    }
}

@Composable
fun MeetingInfo(
    meeting: com.example.voicerecordingapp.data.local.entity.MeetingEntity?,
    totalDuration: String,
    currentChunkInfo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = meeting?.title ?: "Unknown Meeting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (meeting != null) {
                Text(
                    text = TimeUtils.formatDate(meeting.startTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Total Duration: $totalDuration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = currentChunkInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProgressSection(
    currentPosition: Long,
    duration: Long,
    isPrepared: Boolean,
    onSeek: (Long) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeUtils.formatDuration(currentPosition),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = TimeUtils.formatDuration(duration),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // Progress slider
        if (isPrepared && duration > 0) {
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isPrepared: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous chunk button
        IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious
        ) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Chunk",
                tint = if (canGoPrevious) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        
        // Stop button
        IconButton(
            onClick = onStop,
            enabled = isPrepared
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Stop",
                tint = if (isPrepared) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        
        // Play/Pause button
        FloatingActionButton(
            onClick = if (isPlaying) onPause else onPlay,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.ThumbUp else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Next chunk button
        IconButton(
            onClick = onNext,
            enabled = canGoNext
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Chunk",
                tint = if (canGoNext) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ChunkNavigation(
    chunks: List<com.example.voicerecordingapp.data.local.entity.AudioChunkEntity>,
    currentChunkIndex: Int,
    onChunkSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Audio Chunks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chunks.size) { index ->
                    val chunk = chunks[index]
                    val isSelected = index == currentChunkIndex
                    
                    Surface(
                        onClick = { onChunkSelected(index) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Chunk ${index + 1}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// Helper functions
private fun formatTotalDuration(chunks: List<com.example.voicerecordingapp.data.local.entity.AudioChunkEntity>): String {
    val totalMs = chunks.sumOf { it.duration }
    return TimeUtils.formatDuration(totalMs)
}

private fun formatCurrentChunkInfo(currentIndex: Int, totalChunks: Int): String {
    return "Chunk ${currentIndex + 1} of $totalChunks"
}
