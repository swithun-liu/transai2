package com.example.transai.platform

actual fun shouldUseAiProxy(): Boolean = false

actual fun aiProxyEndpoint(): String = "/api/chat/completions"
