package com.example.voicerecordingapp.ui.screens

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicerecordingapp.ui.viewmodel.RecordingViewModel
import com.example.voicerecordingapp.util.PermissionUtils
import com.example.voicerecordingapp.util.TimeUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Permission handling - request both audio and phone permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.getRequiredPermissions()
    )
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        val audioPermission = permissionsState.permissions.find { 
            it.permission == Manifest.permission.RECORD_AUDIO 
        }
        val audioGranted = audioPermission?.status?.isGranted ?: false
        viewModel.setPermissionGranted(audioGranted)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                // Timer display
                Text(
                    text = TimeUtils.formatDuration(uiState.elapsedTime),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Status message
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Record/Stop button
                if (!permissionsState.allPermissionsGranted) {
                    // Show permission UI
                    PermissionUI(
                        shouldShowRationale = permissionsState.permissions.any { it.status.shouldShowRationale },
                        onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
                    )
                } else {
                    // Show recording controls
                    RecordingButton(
                        isRecording = uiState.isRecording,
                        isPaused = uiState.isPaused,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { 
                            viewModel.stopRecording()
                            onNavigateBack()
                        }
                    )
                    
                    if (uiState.isPaused) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resumeRecording() },
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Resume")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionUI(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (shouldShowRationale) {
            Text(
                text = "Microphone and phone permissions are required to record audio and manage phone state",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.width(200.dp)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    isPaused: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // Outer pulsing circle when recording
        if (isRecording && !isPaused) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            )
        }
        
        // Main button
        Surface(
            modifier = Modifier
                .size(150.dp),
            shape = CircleShape,
            color = if (isRecording) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            onClick = {
                if (isRecording) {
                    onStopRecording()
                } else {
                    onStartRecording()
                }
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isRecording) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        modifier = Modifier.size(80.dp),
                        tint = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Record",
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = if (isRecording) "Tap to stop" else "Tap to start recording",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

