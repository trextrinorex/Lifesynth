package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRhythmDao {

    @Query("SELECT * FROM daily_rhythms ORDER BY timestamp DESC")
    fun getAllRhythms(): Flow<List<DailyRhythmEntity>>

    @Query("SELECT * FROM daily_rhythms WHERE dateString = :dateString LIMIT 1")
    suspend fun getRhythmByDate(dateString: String): DailyRhythmEntity?

    @Query("SELECT * FROM daily_rhythms ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRhythms(limit: Int): Flow<List<DailyRhythmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyRhythmEntity)

    @Query("DELETE FROM daily_rhythms")
    suspend fun deleteAll()
}
