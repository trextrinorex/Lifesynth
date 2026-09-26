package com.example.model

enum class ConfidenceLevel(val label: String) {
    HIGH("High Confidence"),
    MEDIUM("Medium Confidence"),
    LOW("Low Confidence")
}

data class UsageSession(
    val id: String,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long
)

data class InactivityPeriod(
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val isNighttime: Boolean = false
)

data class NighttimeInactivityEstimate(
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val lastActivityAppName: String,
    val lastActivityTime: Long,
    val firstActivityAppName: String,
    val firstActivityTime: Long,
    val confidence: ConfidenceLevel,
    val confidenceReason: String
)

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val durationMs: Long,
    val launchCount: Int = 1
)

data class YouVsYouMetric(
    val title: String,
    val todayValueFormatted: String,
    val averageValueFormatted: String,
    val differenceFormatted: String,
    val isPositiveTrend: Boolean,
    val subtitle: String
)

data class DailyRhythmSummary(
    val dateMillis: Long,
    val hasUsageAccess: Boolean,
    val totalScreenTimeMs: Long,
    val totalPhoneFreeTimeMs: Long,
    val longestBreakMs: Long,
    val breakCount: Int,
    val nighttimeInactivity: NighttimeInactivityEstimate?,
    val inactivityEstimationStatus: String,
    val topApps: List<AppUsageStat>,
    val sessions: List<UsageSession>,
    val inactivityPeriods: List<InactivityPeriod>,
    val rhythmScore: Int?,
    val stepCount: Int?,
    val distanceMeters: Float?,
    val activeMinutes: Int?,
    val isRealData: Boolean,
    val rawEventsCount: Int,
    val lastEventTime: Long?
)

data class DebugDataInfo(
    val isUsageAccessGranted: Boolean,
    val lastUsageEventTimeFormatted: String,
    val rawEventsCount: Int,
    val sessionsCount: Int,
    val isHealthConnectConnected: Boolean,
    val stepsRetrievedFormatted: String,
    val databaseRecordsCount: Int,
    val currentAppVersion: String
)
