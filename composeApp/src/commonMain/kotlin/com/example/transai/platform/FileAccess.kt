package com.example.transai.platform

expect class ZipArchive(filePath: String) {
    fun close()
    fun getEntry(name: String): ByteArray?
    fun entryNames(): List<String>
}
