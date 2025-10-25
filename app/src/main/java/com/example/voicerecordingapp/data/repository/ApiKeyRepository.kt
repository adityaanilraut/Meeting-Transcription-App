package com.example.voicerecordingapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _hasApiKey = MutableStateFlow(hasApiKeySync())
    val hasApiKey: Flow<Boolean> = _hasApiKey.asStateFlow()
    
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(API_KEY_PREF, apiKey)
            .apply()
        _hasApiKey.value = true
    }
    
    fun getApiKey(): String? {
        return sharedPreferences.getString(API_KEY_PREF, null)
    }
    
    private fun hasApiKeySync(): Boolean {
        return getApiKey()?.isNotBlank() == true
    }
    
    fun clearApiKey() {
        sharedPreferences.edit()
            .remove(API_KEY_PREF)
            .apply()
        _hasApiKey.value = false
    }
    
    companion object {
        private const val PREFS_NAME = "api_key_prefs"
        private const val API_KEY_PREF = "openai_api_key"
    }
}
