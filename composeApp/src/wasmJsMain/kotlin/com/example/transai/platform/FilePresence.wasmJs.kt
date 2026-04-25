package com.example.transai.platform

actual fun fileExists(path: String): Boolean = WebBridge.readFile(path) != null
