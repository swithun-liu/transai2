package com.example.transai.platform

import java.io.File

actual fun fileExists(path: String): Boolean = File(path).exists()
