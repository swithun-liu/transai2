package com.example.transai.data

import com.example.transai.data.model.BookMetadata
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BookRepository {
    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }
    private val booksKey = "saved_books"

    private val _books = MutableStateFlow(loadBooks())
    val books: StateFlow<List<BookMetadata>> = _books.asStateFlow()

    private fun loadBooks(): List<BookMetadata> {
        val booksString = settings.getString(booksKey, "[]")
        return try {
            json.decodeFromString(booksString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addBook(path: String, title: String) {
        val currentBooks = _books.value.toMutableList()
        // Check if book already exists
        if (currentBooks.none { it.filePath == path }) {
            val newBook = BookMetadata(
                filePath = path,
                title = title
            )
            currentBooks.add(0, newBook) // Add to top
            saveBooks(currentBooks)
        }
    }
    
    fun removeBook(path: String) {
        val currentBooks = _books.value.toMutableList()
        currentBooks.removeAll { it.filePath == path }
        saveBooks(currentBooks)
    }

    fun updateProgress(path: String, position: Int, total: Int) {
        val currentBooks = _books.value.toMutableList()
        val index = currentBooks.indexOfFirst { it.filePath == path }
        if (index != -1) {
            val book = currentBooks[index]
            currentBooks[index] = book.copy(
                lastReadPosition = position,
                totalParagraphs = total
            )
            saveBooks(currentBooks)
        }
    }

    fun getBook(path: String): BookMetadata? {
        return _books.value.find { it.filePath == path }
    }

    private fun saveBooks(books: List<BookMetadata>) {
        val booksString = json.encodeToString(books)
        settings[booksKey] = booksString
        _books.value = books
    }
}
