package com.example.transai.domain.usecase

import com.example.transai.data.BookRepository
import com.example.transai.data.model.BookMetadata

class GetBookMetadataUseCase(private val repository: BookRepository = BookRepository) {
    operator fun invoke(path: String): BookMetadata? {
        return repository.getBook(path)
    }
}
