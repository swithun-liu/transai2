package com.example.transai.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslationConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-3.5-turbo"
)
