package com.example.voicerecordingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.voicerecordingapp.data.repository.ApiKeyRepository
import com.example.voicerecordingapp.ui.navigation.AppNavigation
import com.example.voicerecordingapp.ui.theme.VoiceRecordingAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var apiKeyRepository: ApiKeyRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceRecordingAppTheme {
                AppNavigation(apiKeyRepository = apiKeyRepository)
            }
        }
    }
}