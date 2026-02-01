package com.example.transai.domain.usecase

import com.example.transai.data.SettingsRepository
import com.example.transai.model.TranslationConfig

class UpdateSettingsUseCase(
    private val repository: SettingsRepository = SettingsRepository()
) {
    operator fun invoke(config: TranslationConfig) {
        repository.saveConfig(config)
    }

    fun saveApiKeyForProvider(provider: String, apiKey: String) {
        repository.saveApiKeyForProvider(provider, apiKey)
    }
}
