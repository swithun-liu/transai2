package com.example.transai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transai.data.BookRepository
import com.example.transai.data.model.BookMetadata
import com.example.transai.domain.usecase.ParseBookUseCase
import com.example.transai.platform.saveTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import transai.composeapp.generated.resources.Res

class BookshelfViewModel(
    private val bookRepository: BookRepository = BookRepository(),
    private val parseBookUseCase: ParseBookUseCase = ParseBookUseCase()
) : ViewModel() {

    val books: StateFlow<List<BookMetadata>> = bookRepository.books
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkAndLoadSampleBook()
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun checkAndLoadSampleBook() {
        viewModelScope.launch(Dispatchers.IO) {
            // Only add sample book if the bookshelf is empty
            if (bookRepository.books.value.isEmpty()) {
                try {
                    val bytes = Res.readBytes("files/sample.epub")
                    val path = saveTempFile("sample.epub", bytes)
                    addBook(path)
                } catch (e: Exception) {
                    println("Error loading sample book: ${e.message}")
                }
            }
        }
    }

    fun addBook(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val book = parseBookUseCase(path)
                val title = book.title.ifBlank { 
                     // Fallback to filename if title is empty
                     path.substringAfterLast('/').substringAfterLast('\\')
                }
                bookRepository.addBook(path, title)
            } catch (e: Exception) {
                // Fallback to filename on error
                val filename = path.substringAfterLast('/').substringAfterLast('\\')
                bookRepository.addBook(path, filename)
            }
        }
    }
    
    fun removeBook(path: String) {
        bookRepository.removeBook(path)
    }
}
