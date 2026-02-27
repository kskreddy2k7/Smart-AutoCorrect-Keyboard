package com.smartautocorrect.keyboard.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for user-typed words. Stores frequency data to improve suggestions.
 */
@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey
    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "frequency")
    val frequency: Int = 1,

    @ColumnInfo(name = "last_used")
    val lastUsed: Long = System.currentTimeMillis()
)
