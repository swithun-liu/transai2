package com.example.transai.platform

import platform.Foundation.*
import kotlinx.cinterop.*

actual class ZipArchive actual constructor(filePath: String) {
    actual fun close() {
        // TODO: Implement iOS zip handling
    }

    actual fun getEntry(name: String): ByteArray? {
        // TODO: Implement iOS zip handling
        return null
    }

    actual fun entryNames(): List<String> {
        return emptyList()
    }
}

actual fun saveTempFile(name: String, content: ByteArray): String {
    val tempDir = NSTemporaryDirectory()
    val path = "${tempDir}${name}"
    
    val data = content.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), content.size.toULong())
    }
    
    data.writeToFile(path, true)
    return path
}
