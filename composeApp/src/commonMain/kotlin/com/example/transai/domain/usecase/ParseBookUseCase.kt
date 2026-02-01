package com.example.transai.domain.usecase

import com.example.transai.data.model.Book
import com.example.transai.data.parser.BookParser
import com.example.transai.data.parser.BookParserImpl

class ParseBookUseCase(
    private val parser: BookParser = BookParserImpl()
) {
    operator fun invoke(filePath: String): Book {
        return parser.parse(filePath)
    }
}
