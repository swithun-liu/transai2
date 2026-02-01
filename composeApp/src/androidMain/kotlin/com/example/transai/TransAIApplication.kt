package com.example.transai

import android.app.Application
import android.content.Context

class TransAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        var appContext: Context? = null
            private set
    }
}
