package com.example.transai.platform

import platform.Foundation.NSFileManager

actual fun fileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)
