package com.example.transai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transai.data.BookRepository
import com.example.transai.data.model.BookMetadata
import com.example.transai.domain.usecase.ParseBookUseCase
import com.example.transai.platform.saveTempFile
import com.example.transai.platform.saveBookToSandbox
import com.example.transai.platform.deleteFile
import com.example.transai.platform.openInExplorer
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import transai.composeapp.generated.resources.Res

class BookshelfViewModel(
    private val bookRepository: BookRepository = BookRepository,
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
                    val tempPath = saveTempFile("sample.epub", bytes)
                    // addBook will handle copying to sandbox
                    addBook(tempPath)
                } catch (e: Exception) {
                    println("Error loading sample book: ${e.message}")
                }
            }
        }
    }

    fun addBook(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Copy file to sandbox first
                val savedPath = saveBookToSandbox(path)
                
                val book = parseBookUseCase(savedPath)
                val title = book.title.ifBlank { 
                     // Fallback to filename if title is empty
                     savedPath.substringAfterLast('/').substringAfterLast('\\')
                }
                bookRepository.addBook(savedPath, title)
            } catch (e: Exception) {
                // Fallback to filename on error
                println("Error adding book: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun openBookFolder(path: String) {
        openInExplorer(path)
    }

    fun removeBook(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try to delete physical file first
                deleteFile(path)
            } catch (e: Exception) {
                println("Error deleting file: ${e.message}")
            }
            // Remove from repository (UI) regardless of file deletion success
            bookRepository.removeBook(path)
        }
    }
}
