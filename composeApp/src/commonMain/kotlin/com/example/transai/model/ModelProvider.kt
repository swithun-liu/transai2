package com.example.transai.model

enum class ModelProvider(
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String
) {
    OpenAI("OpenAI", "gpt-3.5-turbo", "https://api.openai.com/v1"),
    DeepSeek("DeepSeek", "deepseek-chat", "https://api.deepseek.com"),
    Gemini("Gemini", "gemini-1.5-flash", "https://generativelanguage.googleapis.com/v1beta/openai/");
    
    companion object {
        fun fromModel(model: String): ModelProvider? {
            return entries.find { it.defaultModel == model }
        }
    }
}
