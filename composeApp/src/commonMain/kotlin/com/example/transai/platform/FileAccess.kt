package com.example.transai.platform

expect class ZipArchive(filePath: String) {
    fun close()
    fun getEntry(name: String): ByteArray?
    fun entryNames(): List<String>
}

expect fun saveTempFile(name: String, content: ByteArray): String

expect fun saveBookToSandbox(sourcePath: String): String

expect fun openInExplorer(path: String)


