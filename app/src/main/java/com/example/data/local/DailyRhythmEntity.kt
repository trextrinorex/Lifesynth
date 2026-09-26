package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_rhythms")
data class DailyRhythmEntity(
    @PrimaryKey
    val dateString: String,
    val screenTimeMs: Long,
    val phoneFreeMs: Long,
    val nightDurationMs: Long,
    val nightStartTime: Long,
    val nightEndTime: Long,
    val confidence: String,
    val rhythmScore: Int,
    val stepCount: Int,
    val longestBreakMs: Long,
    val breaksCount: Int,
    val timestamp: Long
)
