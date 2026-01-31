package com.example.transai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform