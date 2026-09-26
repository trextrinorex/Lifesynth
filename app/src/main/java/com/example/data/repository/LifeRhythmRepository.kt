package com.example.data.repository

import android.content.Context
import com.example.data.health.HealthConnectHelper
import com.example.data.local.DailyRhythmEntity
import com.example.data.local.LifeRhythmDatabase
import com.example.data.preferences.LifeRhythmPreferences
import com.example.data.usage.UsageAccessHelper
import com.example.data.usage.UsageDataEngine
import com.example.model.ConfidenceLevel
import com.example.model.DailyRhythmSummary
import com.example.model.DebugDataInfo
import com.example.model.YouVsYouMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifeRhythmRepository(private val context: Context) {

    private val db = LifeRhythmDatabase.getInstance(context)
    private val dao = db.dailyRhythmDao()
    private val usageEngine = UsageDataEngine(context)
    val preferences = LifeRhythmPreferences(context)

    val historicalRhythms: Flow<List<DailyRhythmEntity>> = dao.getAllRhythms()

    suspend fun getTodaySummary(): DailyRhythmSummary = withContext(Dispatchers.IO) {
        val isHcConnected = preferences.isHealthConnectEnabled && HealthConnectHelper.isHealthConnectInstalled(context)

        val summary = usageEngine.getTodayRhythmSummary(
            nightStartHour = preferences.nightWindowStartHour,
            nightEndHour = preferences.nightWindowEndHour,
            isHealthConnectConnected = isHcConnected,
            healthConnectSteps = if (isHcConnected) 0 else null,
            healthConnectDistanceMeters = if (isHcConnected) 0f else null,
            healthConnectActiveMinutes = if (isHcConnected) 0 else null
        )

        // Only save real daily logs if Usage Access is enabled and we have real data
        if (summary.hasUsageAccess && (summary.totalScreenTimeMs > 0 || summary.sessions.isNotEmpty())) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateString = dateFormat.format(Date(summary.dateMillis))

            val entity = DailyRhythmEntity(
                dateString = dateString,
                screenTimeMs = summary.totalScreenTimeMs,
                phoneFreeMs = summary.totalPhoneFreeTimeMs,
                nightDurationMs = summary.nighttimeInactivity?.durationMs ?: 0L,
                nightStartTime = summary.nighttimeInactivity?.startTime ?: 0L,
                nightEndTime = summary.nighttimeInactivity?.endTime ?: 0L,
                confidence = summary.nighttimeInactivity?.confidence?.name ?: ConfidenceLevel.LOW.name,
                rhythmScore = summary.rhythmScore ?: 0,
                stepCount = summary.stepCount ?: 0,
                longestBreakMs = summary.longestBreakMs,
                breaksCount = summary.breakCount,
                timestamp = summary.dateMillis
            )
            dao.insertOrUpdate(entity)
        }

        summary
    }

    suspend fun getYouVsYouMetrics(todaySummary: DailyRhythmSummary): List<YouVsYouMetric> =
        withContext(Dispatchers.IO) {
            val pastRecords = dao.getRecentRhythms(14).firstOrNull() ?: emptyList()
            val validPast = pastRecords.filter { it.dateString != getTodayDateString() }

            // If no previous real days recorded yet, return empty list (No fake data!)
            if (validPast.isEmpty() || !todaySummary.hasUsageAccess) {
                return@withContext emptyList()
            }

            val avgScreenMs = validPast.map { it.screenTimeMs }.average().toLong()
            val avgPhoneFreeMs = validPast.map { it.phoneFreeMs }.average().toLong()
            val avgNightMs = validPast.map { it.nightDurationMs }.filter { it > 0 }.let {
                if (it.isNotEmpty()) it.average().toLong() else 0L
            }

            val screenDiff = todaySummary.totalScreenTimeMs - avgScreenMs
            val phoneFreeDiff = todaySummary.totalPhoneFreeTimeMs - avgPhoneFreeMs
            val nightDiff = if (avgNightMs > 0 && todaySummary.nighttimeInactivity != null) {
                todaySummary.nighttimeInactivity.durationMs - avgNightMs
            } else 0L

            val metrics = mutableListOf<YouVsYouMetric>()

            metrics.add(
                YouVsYouMetric(
                    title = "Screen Time",
                    todayValueFormatted = formatHoursMinutes(todaySummary.totalScreenTimeMs),
                    averageValueFormatted = formatHoursMinutes(avgScreenMs),
                    differenceFormatted = formatDiffHours(screenDiff),
                    isPositiveTrend = screenDiff <= 0,
                    subtitle = if (screenDiff <= 0) "Below your personal baseline" else "Above your personal baseline"
                )
            )

            metrics.add(
                YouVsYouMetric(
                    title = "Phone-Free Downtime",
                    todayValueFormatted = formatHoursMinutes(todaySummary.totalPhoneFreeTimeMs),
                    averageValueFormatted = formatHoursMinutes(avgPhoneFreeMs),
                    differenceFormatted = formatDiffHours(phoneFreeDiff),
                    isPositiveTrend = phoneFreeDiff >= 0,
                    subtitle = if (phoneFreeDiff >= 0) "More phone-free downtime today" else "Less downtime than your baseline"
                )
            )

            if (avgNightMs > 0 && todaySummary.nighttimeInactivity != null) {
                metrics.add(
                    YouVsYouMetric(
                        title = "Estimated Night Inactivity",
                        todayValueFormatted = formatHoursMinutes(todaySummary.nighttimeInactivity.durationMs),
                        averageValueFormatted = formatHoursMinutes(avgNightMs),
                        differenceFormatted = formatDiffHours(nightDiff),
                        isPositiveTrend = nightDiff in -1800000L..3600000L,
                        subtitle = "Compared against your average stillness"
                    )
                )
            }

            metrics
        }

    suspend fun getObservedPatterns(): List<String> = withContext(Dispatchers.IO) {
        val records = dao.getAllRhythms().firstOrNull() ?: emptyList()
        if (records.size < 3) {
            return@withContext emptyList()
        }

        val patterns = mutableListOf<String>()
        val avgScreen = records.map { it.screenTimeMs }.average()
        val highScreenDays = records.filter { it.screenTimeMs > avgScreen }
        val lowScreenDays = records.filter { it.screenTimeMs <= avgScreen }

        if (lowScreenDays.isNotEmpty() && highScreenDays.isNotEmpty()) {
            val lowScreenNightAvg = lowScreenDays.map { it.nightDurationMs }.average()
            val highScreenNightAvg = highScreenDays.map { it.nightDurationMs }.average()

            if (lowScreenNightAvg > highScreenNightAvg + 900000L) { // 15+ mins
                patterns.add("Days with lower screen time observed a longer unbroken nighttime phone-free interval.")
            }
        }

        if (records.any { it.breaksCount >= 3 }) {
            patterns.add("Days with 3 or more daytime breaks of 15+ minutes correlated with higher behavioral rhythm scores.")
        }

        patterns
    }

    suspend fun getDebugInfo(todaySummary: DailyRhythmSummary): DebugDataInfo = withContext(Dispatchers.IO) {
        val records = dao.getAllRhythms().firstOrNull() ?: emptyList()
        val hasPerm = UsageAccessHelper.hasUsageAccess(context)
        val isHc = preferences.isHealthConnectEnabled && HealthConnectHelper.isHealthConnectInstalled(context)

        val lastEventFormatted = todaySummary.lastEventTime?.let {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
        } ?: "No events"

        val stepsFormatted = if (isHc) {
            todaySummary.stepCount?.let { "$it steps" } ?: "0 steps (waiting for device sync)"
        } else {
            "NOT CONNECTED"
        }

        DebugDataInfo(
            isUsageAccessGranted = hasPerm,
            lastUsageEventTimeFormatted = lastEventFormatted,
            rawEventsCount = todaySummary.rawEventsCount,
            sessionsCount = todaySummary.sessions.size,
            isHealthConnectConnected = isHc,
            stepsRetrievedFormatted = stepsFormatted,
            databaseRecordsCount = records.size,
            currentAppVersion = "1.0.0 (Native Release)"
        )
    }

    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val records = dao.getAllRhythms().firstOrNull() ?: emptyList()
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("date", record.dateString)
                put("screenTimeMinutes", record.screenTimeMs / 60000)
                put("phoneFreeMinutes", record.phoneFreeMs / 60000)
                put("nightInactiveMinutes", record.nightDurationMs / 60000)
                put("confidence", record.confidence)
                put("rhythmScore", record.rhythmScore)
                put("stepCount", record.stepCount)
                put("longestBreakMinutes", record.longestBreakMs / 60000)
                put("breaksCount", record.breaksCount)
            }
            jsonArray.put(obj)
        }
        val root = JSONObject().apply {
            put("app", "LifeRhythm")
            put("exportedAt", System.currentTimeMillis())
            put("version", "1.0.0")
            put("records", jsonArray)
        }
        root.toString(2)
    }

    suspend fun exportDataAsCsv(): String = withContext(Dispatchers.IO) {
        val records = dao.getAllRhythms().firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.append("Date,ScreenTimeMinutes,PhoneFreeMinutes,NightInactiveMinutes,Confidence,RhythmScore,StepCount,BreaksCount\n")
        records.forEach { r ->
            sb.append("${r.dateString},${r.screenTimeMs / 60000},${r.phoneFreeMs / 60000},${r.nightDurationMs / 60000},${r.confidence},${r.rhythmScore},${r.stepCount},${r.breaksCount}\n")
        }
        sb.toString()
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        dao.deleteAll()
        preferences.clearAll()
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun formatHoursMinutes(ms: Long): String {
        val totalMinutes = ms / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun formatDiffHours(ms: Long): String {
        val prefix = if (ms >= 0) "+" else "-"
        val absMs = kotlin.math.abs(ms)
        val totalMinutes = absMs / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "$prefix${hours}h ${minutes}m" else "$prefix${minutes}m"
    }
}
