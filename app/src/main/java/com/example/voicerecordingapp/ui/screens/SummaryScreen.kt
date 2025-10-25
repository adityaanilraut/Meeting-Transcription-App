package com.example.voicerecordingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicerecordingapp.ui.viewmodel.SummaryUiState
import com.example.voicerecordingapp.ui.viewmodel.SummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is SummaryUiState.Loading -> {
                LoadingState(modifier = Modifier.padding(paddingValues))
            }
            is SummaryUiState.Success -> {
                SummaryContent(
                    state = state,
                    onUpdateTitle = { viewModel.updateMeetingTitle(it) },
                    onRetrySummary = { viewModel.retrySummary() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SummaryUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Go Back")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryContent(
    state: SummaryUiState.Success,
    onUpdateTitle: (String) -> Unit,
    onRetrySummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(state.meeting.title) }
    var transcriptExpanded by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { isEditingTitle = !isEditingTitle }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit title")
                        }
                    }
                    
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { 
                                isEditingTitle = false
                                editedTitle = state.meeting.title
                            }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = { 
                                onUpdateTitle(editedTitle)
                                isEditingTitle = false
                            }) {
                                Text("Save")
                            }
                        }
                    } else {
                        Text(
                            text = state.meeting.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Summary section
        item {
            SummarySection(
                state = state,
                onRetrySummary = onRetrySummary
            )
        }
        
        // Transcript section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Transcript",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { transcriptExpanded = !transcriptExpanded }) {
                            Icon(
                                if (transcriptExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (transcriptExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                    
                    if (transcriptExpanded) {
                        if (state.transcripts.isEmpty()) {
                            Text(
                                text = state.message ?: "No transcript available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.transcripts.forEach { transcript ->
                                    Text(
                                        text = transcript.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummarySection(
    state: SummaryUiState.Success,
    onRetrySummary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                state.isGenerating -> {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = "Generating summary...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show streaming content if available
                    if (state.streamingContent != null) {
                        Text(
                            text = state.streamingContent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                state.error != null -> {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Button(onClick = onRetrySummary) {
                        Text("Retry")
                    }
                }
                
                state.summary != null -> {
                    // Summary
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.summary.summary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Divider()
                    
                    // Action Items
                    Text(
                        text = "Action Items",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.summary.actionItems.isEmpty()) {
                        Text(
                            text = "No action items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.summary.actionItems.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", style = MaterialTheme.typography.bodyMedium)
                                Text(item, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Key Points
                    Text(
                        text = "Key Points",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.summary.keyPoints.isEmpty()) {
                        Text(
                            text = "No key points",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.summary.keyPoints.forEach { point ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", style = MaterialTheme.typography.bodyMedium)
                                Text(point, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                
                else -> {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.message ?: "Waiting for transcription...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

