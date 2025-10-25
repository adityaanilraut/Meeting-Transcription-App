package com.example.voicerecordingapp.di

import android.content.Context
import androidx.room.Room
import com.example.voicerecordingapp.data.local.AppDatabase
import com.example.voicerecordingapp.data.repository.ApiKeyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_recording_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMeetingDao(database: AppDatabase) = database.meetingDao()
    
    @Provides
    @Singleton
    fun provideAudioChunkDao(database: AppDatabase) = database.audioChunkDao()
    
    @Provides
    @Singleton
    fun provideTranscriptDao(database: AppDatabase) = database.transcriptDao()
    
    @Provides
    @Singleton
    fun provideSummaryDao(database: AppDatabase) = database.summaryDao()
    
    @Provides
    @Singleton
    fun provideApiKeyRepository(
        @ApplicationContext context: Context
    ): ApiKeyRepository {
        return ApiKeyRepository(context)
    }
}

