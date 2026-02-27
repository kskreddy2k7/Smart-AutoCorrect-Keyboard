package com.smartautocorrect.keyboard.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user word frequency tracking.
 */
@Dao
interface UserWordDao {

    /** Insert a new word or replace if it already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: UserWordEntity)

    /** Increment the frequency count for an existing word. */
    @Query("UPDATE user_words SET frequency = frequency + 1, last_used = :timestamp WHERE word = :word")
    suspend fun incrementFrequency(word: String, timestamp: Long = System.currentTimeMillis())

    /** Get word frequency by word text. Returns null if not found. */
    @Query("SELECT * FROM user_words WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): UserWordEntity?

    /** Get top N most frequently used words (for suggestions). */
    @Query("SELECT * FROM user_words ORDER BY frequency DESC LIMIT :limit")
    suspend fun getTopWords(limit: Int): List<UserWordEntity>

    /** Get all user words as a Flow (for observing changes). */
    @Query("SELECT * FROM user_words ORDER BY frequency DESC")
    fun getAllWords(): Flow<List<UserWordEntity>>
}
