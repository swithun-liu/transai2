package com.example.transai

class WasmJsPlatform : Platform {
    override val name: String = "Browser (Wasm)"
}

actual fun getPlatform(): Platform = WasmJsPlatform()
