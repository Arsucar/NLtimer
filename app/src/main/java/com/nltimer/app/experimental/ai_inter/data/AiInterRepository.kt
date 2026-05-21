package com.nltimer.app.experimental.ai_inter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nltimer.core.ai.config.AiConfigProvider
import com.nltimer.core.ai.config.AiInterConfig as CoreAiInterConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_inter_settings")

@Singleton
class AiInterRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val aiCallLogDao: AiCallLogDao
) : AiConfigProvider {
    private val API_ADDRESS = stringPreferencesKey("api_address")
    private val API_PATH = stringPreferencesKey("api_path")
    private val API_KEY = stringPreferencesKey("api_key")
    private val MODEL_NAME = stringPreferencesKey("model_name")
    private val PROMPT_NOTES = stringPreferencesKey("prompt_notes")
    private val PROMPT_TASK_GEN = stringPreferencesKey("prompt_task_gen")
    private val PROMPT_CHAT = stringPreferencesKey("prompt_chat")
    private val PROMPT_SYSTEM = stringPreferencesKey("prompt_system")
    private val MAX_TOOL_ROUNDS = intPreferencesKey("max_tool_rounds")
    private val MAX_BATCH_SIZE = intPreferencesKey("max_batch_size")

    override val config: Flow<AiInterConfig> = context.dataStore.data.map { preferences ->
        AiInterConfig(
            apiAddress = preferences[API_ADDRESS] ?: "https://integrate.api.nvidia.com/v1",
            apiPath = preferences[API_PATH] ?: "/chat/completions",
            apiKey = preferences[API_KEY] ?: "",
            modelName = preferences[MODEL_NAME] ?: "openai/gpt-oss-120b",
            promptNotes = preferences[PROMPT_NOTES] ?: "",
            promptTaskGen = preferences[PROMPT_TASK_GEN] ?: "",
            promptChat = preferences[PROMPT_CHAT] ?: "",
            promptSystem = preferences[PROMPT_SYSTEM] ?: "",
            maxToolRounds = preferences[MAX_TOOL_ROUNDS] ?: 5,
            maxBatchSize = preferences[MAX_BATCH_SIZE] ?: 50,
        )
    }

    override suspend fun updateConfig(update: (AiInterConfig) -> AiInterConfig) {
        context.dataStore.edit { preferences ->
            val current = AiInterConfig(
                apiAddress = preferences[API_ADDRESS] ?: "https://integrate.api.nvidia.com/v1",
                apiPath = preferences[API_PATH] ?: "/chat/completions",
                apiKey = preferences[API_KEY] ?: "",
                modelName = preferences[MODEL_NAME] ?: "openai/gpt-oss-120b",
                promptNotes = preferences[PROMPT_NOTES] ?: "",
                promptTaskGen = preferences[PROMPT_TASK_GEN] ?: "",
                promptChat = preferences[PROMPT_CHAT] ?: "",
                promptSystem = preferences[PROMPT_SYSTEM] ?: "",
                maxToolRounds = preferences[MAX_TOOL_ROUNDS] ?: 5,
                maxBatchSize = preferences[MAX_BATCH_SIZE] ?: 50,
            )
            val updated = update(current)
            preferences[API_ADDRESS] = updated.apiAddress
            preferences[API_PATH] = updated.apiPath
            preferences[API_KEY] = updated.apiKey
            preferences[MODEL_NAME] = updated.modelName
            preferences[PROMPT_NOTES] = updated.promptNotes
            preferences[PROMPT_TASK_GEN] = updated.promptTaskGen
            preferences[PROMPT_CHAT] = updated.promptChat
            preferences[PROMPT_SYSTEM] = updated.promptSystem
            preferences[MAX_TOOL_ROUNDS] = updated.maxToolRounds
            preferences[MAX_BATCH_SIZE] = updated.maxBatchSize
        }
    }

    val allLogs: Flow<List<AiCallLogEntity>> = aiCallLogDao.getAllLogs()

    fun getLogById(id: Long): Flow<AiCallLogEntity?> = aiCallLogDao.getLogById(id)

    suspend fun addLog(log: AiCallLogEntity) {
        aiCallLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        aiCallLogDao.clearLogs()
    }
}
