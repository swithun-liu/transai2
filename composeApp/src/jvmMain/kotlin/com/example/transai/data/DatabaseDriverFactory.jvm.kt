package com.example.transai.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.transai.db.TransAIDatabase
import java.io.File

import app.cash.sqldelight.db.QueryResult

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home")
        val dbDir = File(userHome, ".transai")
        if (!dbDir.exists()) dbDir.mkdirs()
        // Use a new database file for version 2 to avoid migration issues during dev
        val dbFile = File(dbDir, "transai_v2.db")
        val exists = dbFile.exists()
        
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        
        try {
            if (!exists) {
                TransAIDatabase.Schema.create(driver)
                // Set initial version to latest
                driver.execute(null, "PRAGMA user_version = ${TransAIDatabase.Schema.version};", 0)
            } else {
                 val currentVersion = getVersion(driver)
                 if (currentVersion == 0L) {
                     // If version is 0 but file exists, it might be the old version without version tracking
                     // Or it's a fresh DB that didn't get version set.
                     // Since we added a migration from 0 to 1, let's assume it's version 0 and try migrate to 1?
                     // Or if it failed previously, it might be in inconsistent state.
                     // Safe bet: if version is 0 and we expect > 0, try migrate.
                     TransAIDatabase.Schema.migrate(driver, 0, TransAIDatabase.Schema.version)
                 } else if (currentVersion < TransAIDatabase.Schema.version) {
                     TransAIDatabase.Schema.migrate(driver, currentVersion, TransAIDatabase.Schema.version)
                 }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return driver
    }
    
    private fun getVersion(driver: SqlDriver): Long {
        return try {
            val queryResult = driver.executeQuery<Long>(null, "PRAGMA user_version;", mapper = { cursor ->
                if (cursor.next().value) {
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                } else {
                    QueryResult.Value(0L)
                }
            }, parameters = 0)
            queryResult.value
        } catch (e: Exception) {
            0L
        }
    }
}
