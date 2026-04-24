@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.platform

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json

actual class ZipArchive actual constructor(private val filePath: String) {
    private val json = Json

    actual fun close() = Unit

    actual fun getEntry(name: String): ByteArray? {
        val encoded = WebBridge.zipEntryBase64(filePath, name)
        return when (encoded) {
            is String -> Base64.decode(encoded)
            else -> null
        }
    }

    actual fun entryNames(): List<String> {
        val namesJson = WebBridge.zipEntryNames(filePath)
        return when (namesJson) {
            is String -> json.decodeFromString(namesJson)
            else -> emptyList()
        }
    }
}

actual fun saveTempFile(name: String, content: ByteArray): String {
    return BrowserFileStore.saveTempFile(name, content)
}

actual fun saveBookToSandbox(sourcePath: String): String {
    return BrowserFileStore.saveBook(sourcePath)
}

actual fun deleteFile(path: String): Boolean {
    return BrowserFileStore.delete(path)
}

actual fun openInExplorer(path: String) = Unit

actual fun supportsOpenInExplorer(): Boolean = false
