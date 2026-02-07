package com.example.transai.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.transai.TransAIApplication
import com.example.transai.db.TransAIDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val context = TransAIApplication.appContext ?: throw IllegalStateException("Context not initialized")
        return AndroidSqliteDriver(TransAIDatabase.Schema, context, "transai.db")
    }
}
