package com.example.transai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BookMetadata(
    val filePath: String,
    val title: String,
    val lastReadPosition: Int = 0,
    val totalParagraphs: Int = 0
)
