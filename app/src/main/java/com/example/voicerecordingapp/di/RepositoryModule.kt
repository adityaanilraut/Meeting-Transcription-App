package com.example.voicerecordingapp.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repository classes are self-injecting with @Singleton and @Inject constructors
    // No additional providers needed
}
