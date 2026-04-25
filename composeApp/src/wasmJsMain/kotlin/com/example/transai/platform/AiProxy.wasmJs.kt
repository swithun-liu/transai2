@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.example.transai.platform

actual fun shouldUseAiProxy(): Boolean = true

actual fun aiProxyEndpoint(): String {
    val hostname = currentHostname().lowercase()
    return if (hostname == "localhost" || hostname == "127.0.0.1") {
        "http://127.0.0.1:8081/api/chat/completions"
    } else {
        "/api/chat/completions"
    }
}

@JsFun("() => window.location.hostname")
external fun currentHostname(): String
