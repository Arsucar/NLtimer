package com.nltimer.app.experimental.ai_inter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_inter_settings")

@Singleton
class AiInterRepository @Inject constructor(
    private val context: Context,
    private val aiCallLogDao: AiCallLogDao
) {
    private val API_ADDRESS = stringPreferencesKey("api_address")
    private val API_PATH = stringPreferencesKey("api_path")
    private val API_KEY = stringPreferencesKey("api_key")
    private val MODEL_NAME = stringPreferencesKey("model_name")

    val config: Flow<AiInterConfig> = context.dataStore.data.map { preferences ->
        AiInterConfig(
            apiAddress = preferences[API_ADDRESS] ?: "https://integrate.api.nvidia.com/v1",
            apiPath = preferences[API_PATH] ?: "/chat/completions",
            apiKey = preferences[API_KEY] ?: "",
            modelName = preferences[MODEL_NAME] ?: "openai/gpt-oss-120b"
        )
    }

    suspend fun updateConfig(update: (AiInterConfig) -> AiInterConfig) {
        context.dataStore.edit { preferences ->
            val current = AiInterConfig(
                apiAddress = preferences[API_ADDRESS] ?: "https://integrate.api.nvidia.com/v1",
                apiPath = preferences[API_PATH] ?: "/chat/completions",
                apiKey = preferences[API_KEY] ?: "",
                modelName = preferences[MODEL_NAME] ?: "openai/gpt-oss-120b"
            )
            val updated = update(current)
            preferences[API_ADDRESS] = updated.apiAddress
            preferences[API_PATH] = updated.apiPath
            preferences[API_KEY] = updated.apiKey
            preferences[MODEL_NAME] = updated.modelName
        }
    }

    val allLogs: Flow<List<AiCallLogEntity>> = aiCallLogDao.getAllLogs()

    suspend fun addLog(log: AiCallLogEntity) {
        aiCallLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        aiCallLogDao.clearLogs()
    }
}
