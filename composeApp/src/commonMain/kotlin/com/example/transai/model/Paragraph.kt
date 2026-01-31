package com.example.transai.model

data class Paragraph(
    val id: Int,
    val originalText: String,
    val translatedText: String? = null,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
