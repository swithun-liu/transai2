package com.example.transai.domain.usecase

import com.example.transai.data.BookRepository

class UpdateBookProgressUseCase(private val repository: BookRepository = BookRepository) {
    operator fun invoke(path: String, position: Int, total: Int) {
        repository.updateProgress(path, position, total)
    }
}
