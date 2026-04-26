package com.example.transai.data

import com.example.transai.model.CharacterRecognitionSettings
import com.example.transai.model.TranslationConfig
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SettingsRepository {
    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<TranslationConfig> = _config.asStateFlow()

    private fun loadConfig(): TranslationConfig {
        val characterRecognitionSettings = settings
            .getStringOrNull("character_recognition_settings")
            ?.let { raw ->
                runCatching {
                    json.decodeFromString<CharacterRecognitionSettings>(raw)
                }.getOrNull()
            }
            ?: CharacterRecognitionSettings.default()
        return TranslationConfig(
            apiKey = settings.getString("api_key", ""),
            baseUrl = settings.getString("base_url", "https://api.openai.com/v1"),
            model = settings.getString("model_name", "gpt-3.5-turbo"),
            characterRecognitionSettings = characterRecognitionSettings
        )
    }

    fun saveConfig(newConfig: TranslationConfig) {
        settings["api_key"] = newConfig.apiKey
        settings["base_url"] = newConfig.baseUrl
        settings["model_name"] = newConfig.model
        settings["character_recognition_settings"] = json.encodeToString(newConfig.characterRecognitionSettings)
        _config.value = newConfig
    }

    fun getApiKeyForProvider(provider: String): String {
        return settings.getString("api_key_$provider", "")
    }

    fun saveApiKeyForProvider(provider: String, apiKey: String) {
        settings["api_key_$provider"] = apiKey
    }
}
