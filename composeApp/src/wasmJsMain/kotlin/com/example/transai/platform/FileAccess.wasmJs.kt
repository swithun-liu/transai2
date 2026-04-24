@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.platform

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json

actual class ZipArchive actual constructor(private val filePath: String) {
    private val json = Json

    actual fun close() = Unit

    actual fun getEntry(name: String): ByteArray? {
        val encoded = WebBridge.zipEntryBase64(filePath, name) as? String ?: return null
        return Base64.decode(encoded)
    }

    actual fun entryNames(): List<String> {
        val namesJson = WebBridge.zipEntryNames(filePath) as String
        return json.decodeFromString(namesJson)
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
