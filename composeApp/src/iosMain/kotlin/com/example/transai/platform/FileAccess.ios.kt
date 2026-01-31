package com.example.transai.platform

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
