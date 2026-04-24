@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.transai.platform

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json

actual class ZipArchive actual constructor(private val filePath: String) {
    private val json = Json

    actual fun close() = Unit

    actual fun getEntry(name: String): ByteArray? {
    val encoded = WebBridge.zipEntryBase64(filePath, name)
    
    // 更安全的类型检查
    if (encoded == null) {
        return null
    }
    
    try {
        val encodedString = encoded as? String
        if (encodedString != null) {
            return Base64.decode(encodedString)
        }
    } catch (e: Exception) {
        println("Error decoding base64 for $name: $e")
    }
    
    return null
}

actual fun entryNames(): List<String> {
    val namesJson = WebBridge.zipEntryNames(filePath)
    
    // 更安全的类型检查
    if (namesJson == null) {
        return emptyList()
    }
    
    try {
        val jsonString = namesJson as? String
        if (jsonString != null) {
            return json.decodeFromString(jsonString)
        }
    } catch (e: Exception) {
        println("Error parsing zip entry names: $e")
    }
    
    return emptyList()
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
