package com.example.transai.data.model

data class Book(
    val title: String,
    val author: String,
    val chapters: List<Chapter>
)

data class Chapter(
    val title: String,
    val content: String,
    val paragraphs: List<String>
)
