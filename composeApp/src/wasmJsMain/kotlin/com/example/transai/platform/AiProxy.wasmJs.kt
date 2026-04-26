@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.example.transai.platform

@JsModule("./runtime_bridge.mjs")
external object RuntimeBridge {
    fun currentHostname(): String
    fun configuredAiProxyEndpoint(): String
}

actual fun shouldUseAiProxy(): Boolean = true

actual fun aiProxyEndpoint(): String {
    val configured = RuntimeBridge.configuredAiProxyEndpoint().trim()
    if (configured.isNotEmpty()) {
        return configured
    }

    val hostname = RuntimeBridge.currentHostname().lowercase()
    return if (hostname == "localhost" || hostname == "127.0.0.1") {
        "http://127.0.0.1:8081/api/chat/completions"
    } else {
        "/api/chat/completions"
    }
}
