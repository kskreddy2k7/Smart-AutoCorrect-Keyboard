package com.smartautocorrect.keyboard.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main Room database for Smart AutoCorrect Keyboard.
 * Stores user word frequency data for adaptive suggestions.
 */
@Database(
    entities = [UserWordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    /** DAO for user word operations. */
    abstract fun userWordDao(): UserWordDao

    companion object {
        const val DATABASE_NAME = "smart_keyboard_db"
    }
}
