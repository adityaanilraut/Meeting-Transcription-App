package com.example.voicerecordingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicerecordingapp.data.local.dao.MeetingDao
import com.example.voicerecordingapp.data.local.entity.MeetingEntity
import com.example.voicerecordingapp.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meetingDao: MeetingDao,
    private val recordingRepository: RecordingRepository
) : ViewModel() {
    
    val meetings: StateFlow<List<MeetingEntity>> = meetingDao.getAllMeetings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun deleteMeeting(id: Long) {
        viewModelScope.launch {
            recordingRepository.deleteMeeting(id)
        }
    }
}

