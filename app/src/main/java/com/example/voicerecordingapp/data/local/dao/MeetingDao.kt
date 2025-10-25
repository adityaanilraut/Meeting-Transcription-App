package com.example.voicerecordingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.voicerecordingapp.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: MeetingEntity): Long
    
    @Update
    suspend fun update(meeting: MeetingEntity)
    
    @Delete
    suspend fun delete(meeting: MeetingEntity)
    
    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingById(id: Long): MeetingEntity?
    
    @Query("SELECT * FROM meetings WHERE id = :id")
    fun getMeetingByIdFlow(id: Long): Flow<MeetingEntity?>
    
    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>
    
    @Query("SELECT * FROM meetings WHERE status = :status ORDER BY startTime DESC")
    fun getMeetingsByStatus(status: String): Flow<List<MeetingEntity>>
    
    @Query("SELECT * FROM meetings WHERE status = 'Recording' LIMIT 1")
    suspend fun getActiveRecording(): MeetingEntity?
    
    @Query("UPDATE meetings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    
    @Query("UPDATE meetings SET endTime = :endTime, totalDuration = :duration WHERE id = :id")
    suspend fun updateEndTime(id: Long, endTime: Long, duration: Long)
    
    @Query("UPDATE meetings SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)
    
    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteById(id: Long)
}

