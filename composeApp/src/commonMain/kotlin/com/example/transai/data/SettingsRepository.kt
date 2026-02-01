package com.example.transai.data

import com.example.transai.model.TranslationConfig
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsRepository {
    private val settings = Settings()
    
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<TranslationConfig> = _config.asStateFlow()

    private fun loadConfig(): TranslationConfig {
        return TranslationConfig(
            apiKey = settings.getString("api_key", ""),
            baseUrl = settings.getString("base_url", "https://api.openai.com/v1"),
            model = settings.getString("model_name", "gpt-3.5-turbo")
        )
    }

    fun saveConfig(newConfig: TranslationConfig) {
        settings["api_key"] = newConfig.apiKey
        settings["base_url"] = newConfig.baseUrl
        settings["model_name"] = newConfig.model
        _config.value = newConfig
    }

    fun getApiKeyForProvider(provider: String): String {
        return settings.getString("api_key_$provider", "")
    }

    fun saveApiKeyForProvider(provider: String, apiKey: String) {
        settings["api_key_$provider"] = apiKey
    }
}
