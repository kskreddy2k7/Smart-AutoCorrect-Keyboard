package com.smartautocorrect.keyboard.data.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.smartautocorrect.keyboard.data.database.AppDatabase
import com.smartautocorrect.keyboard.data.database.UserWordDao
import com.smartautocorrect.keyboard.data.dictionary.DictionaryRepositoryImpl
import com.smartautocorrect.keyboard.domain.repository.DictionaryRepository
import com.smartautocorrect.keyboard.utils.ThemeManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-scoped dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(
        impl: DictionaryRepositoryImpl
    ): DictionaryRepository

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            ).build()
        }

        @Provides
        @Singleton
        fun provideUserWordDao(database: AppDatabase): UserWordDao {
            return database.userWordDao()
        }

        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

        @Provides
        @Singleton
        fun provideThemeManager(@ApplicationContext context: Context): ThemeManager {
            return ThemeManager(context)
        }
    }
}
