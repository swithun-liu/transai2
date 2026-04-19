@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.platform

import kotlin.io.encoding.Base64

internal object BrowserFileStore {
    fun saveTempFile(name: String, content: ByteArray): String {
        val path = WebBridge.createTempPath(name)
        WebBridge.saveFile(path, Base64.encode(content))
        return path
    }

    fun saveBook(sourcePath: String): String {
        if (sourcePath.startsWith("browser://book/")) {
            return sourcePath
        }

        val encoded = WebBridge.readFile(sourcePath)
            ?: error("File not found in browser storage: $sourcePath")
        val path = WebBridge.createBookPath(fileNameFromPath(sourcePath))
        WebBridge.saveFile(path, encoded)
        WebBridge.deleteStoredFile(sourcePath)
        return path
    }

    fun readBytes(path: String): ByteArray? {
        val encoded = WebBridge.readFile(path) ?: return null
        return Base64.decode(encoded)
    }

    fun delete(path: String): Boolean = WebBridge.deleteStoredFile(path)

    private fun fileNameFromPath(path: String): String {
        return path.substringAfterLast('/').ifBlank { "book.epub" }
    }
}
