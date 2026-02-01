package com.example.transai.domain.usecase

import com.example.transai.data.SettingsRepository
import com.example.transai.model.TranslationConfig
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val repository: SettingsRepository = SettingsRepository()
) {
    operator fun invoke(): Flow<TranslationConfig> {
        return repository.config
    }

    fun getApiKeyForProvider(provider: String): String {
        return repository.getApiKeyForProvider(provider)
    }
}
