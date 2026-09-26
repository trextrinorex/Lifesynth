package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyRhythmEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LifeRhythmDatabase : RoomDatabase() {

    abstract fun dailyRhythmDao(): DailyRhythmDao

    companion object {
        @Volatile
        private var INSTANCE: LifeRhythmDatabase? = null

        fun getInstance(context: Context): LifeRhythmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeRhythmDatabase::class.java,
                    "liferhythm.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
