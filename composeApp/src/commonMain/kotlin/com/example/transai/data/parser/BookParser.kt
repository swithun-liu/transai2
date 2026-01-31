package com.example.transai.data.parser

import com.example.transai.data.model.Book

interface BookParser {
    fun parse(filePath: String): Book
}

class BookParserImpl : BookParser {
    private val epubParser = EpubParser()

    override fun parse(filePath: String): Book {
        return when {
            filePath.endsWith(".epub", ignoreCase = true) -> epubParser.parse(filePath)
            filePath.endsWith(".txt", ignoreCase = true) -> parseTxt(filePath)
            else -> throw IllegalArgumentException("Unsupported format: $filePath")
        }
    }

    private fun parseTxt(filePath: String): Book {
        // TODO: Implement TXT parsing with charset detection
        return Book("Unknown", "Unknown", emptyList())
    }
}
