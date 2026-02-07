package com.example.transai.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.transai.db.TransAIDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home")
        val dbDir = File(userHome, ".transai")
        if (!dbDir.exists()) dbDir.mkdirs()
        val dbFile = File(dbDir, "transai.db")
        val exists = dbFile.exists()
        
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        
        if (!exists) {
            TransAIDatabase.Schema.create(driver)
        }
        return driver
    }
}
