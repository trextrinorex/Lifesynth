package com.example.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.model.AppUsageStat
import com.example.model.ConfidenceLevel
import com.example.model.DailyRhythmSummary
import com.example.model.InactivityPeriod
import com.example.model.NighttimeInactivityEstimate
import com.example.model.UsageSession
import java.util.Calendar
import java.util.UUID

class UsageDataEngine(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager = context.packageManager

    data class RawEvent(
        val packageName: String,
        val timestamp: Long,
        val eventType: Int
    )

    fun getTodayRhythmSummary(
        nightStartHour: Int = 21, // 9:00 PM
        nightEndHour: Int = 11,   // 11:00 AM
        isHealthConnectConnected: Boolean = false,
        healthConnectSteps: Int? = null,
        healthConnectDistanceMeters: Float? = null,
        healthConnectActiveMinutes: Int? = null
    ): DailyRhythmSummary {
        val hasPermission = UsageAccessHelper.hasUsageAccess(context)
        val now = System.currentTimeMillis()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        if (!hasPermission || usageStatsManager == null) {
            return DailyRhythmSummary(
                dateMillis = startOfDay,
                hasUsageAccess = false,
                totalScreenTimeMs = 0L,
                totalPhoneFreeTimeMs = 0L,
                longestBreakMs = 0L,
                breakCount = 0,
                nighttimeInactivity = null,
                inactivityEstimationStatus = "Usage Access permission is required to analyze device rhythms.",
                topApps = emptyList(),
                sessions = emptyList(),
                inactivityPeriods = emptyList(),
                rhythmScore = null,
                stepCount = if (isHealthConnectConnected) healthConnectSteps else null,
                distanceMeters = if (isHealthConnectConnected) healthConnectDistanceMeters else null,
                activeMinutes = if (isHealthConnectConnected) healthConnectActiveMinutes else null,
                isRealData = false,
                rawEventsCount = 0,
                lastEventTime = null
            )
        }

        // Window for nighttime: yesterday at nightStartHour to today at nightEndHour
        val nightCal = Calendar.getInstance().apply {
            timeInMillis = startOfDay
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, nightStartHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val queryStartTime = nightCal.timeInMillis

        // 1. Query REAL usage events
        val rawEvents = queryRawUsageEvents(queryStartTime, now)
        val lastEventTimestamp = rawEvents.maxOfOrNull { it.timestamp }

        // 2. Query aggregate UsageStats for verification & total foreground screen time
        val dailyStats = queryDailyUsageStats(startOfDay, now)

        // 3. Convert raw events to continuous usage sessions
        val allSessions = convertRawEventsToSessions(rawEvents, now)

        // Filter sessions that took place today (since midnight)
        val todaySessions = allSessions.filter { it.endTime >= startOfDay }

        // Calculate total screen time from real usage
        var totalScreenTimeMs = todaySessions.sumOf { session ->
            val effectiveStart = maxOf(session.startTime, startOfDay)
            val effectiveEnd = minOf(session.endTime, now)
            if (effectiveEnd > effectiveStart) effectiveEnd - effectiveStart else 0L
        }

        // Complement with UsageStats foreground time if event stream was incomplete
        val totalForegroundFromStats = dailyStats.sumOf { it.totalTimeInForeground }
        if (totalForegroundFromStats > totalScreenTimeMs) {
            totalScreenTimeMs = totalForegroundFromStats
        }

        val elapsedToday = now - startOfDay
        val totalPhoneFreeTimeMs = maxOf(0L, elapsedToday - totalScreenTimeMs)

        // 4. Calculate real inactivity intervals today
        val inactivityPeriods = calculateRealInactivityPeriods(todaySessions, startOfDay, now)
        val breaksOver15Min = inactivityPeriods.filter { it.durationMs >= 15 * 60 * 1000L }
        val longestBreakMs = inactivityPeriods.maxOfOrNull { it.durationMs } ?: 0L

        // 5. Calculate real nighttime inactivity
        val nightWindowEndCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nightEndHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nightWindowEndTime = minOf(now, nightWindowEndCal.timeInMillis)
        val (nighttimeEstimate, nightStatus) = calculateRealNighttimeInactivity(
            allSessions = allSessions,
            windowStart = queryStartTime,
            windowEnd = nightWindowEndTime
        )

        // 6. Aggregate real top apps
        val topApps = aggregateRealTopApps(todaySessions, dailyStats)

        // 7. Calculate Behavioral Rhythm Score (only if real data exists)
        val rhythmScore = if (totalScreenTimeMs > 0 || todaySessions.isNotEmpty()) {
            calculateRealRhythmScore(
                screenTimeMs = totalScreenTimeMs,
                phoneFreeMs = totalPhoneFreeTimeMs,
                breaksCount = breaksOver15Min.size,
                nighttimeEstimate = nighttimeEstimate
            )
        } else {
            null
        }

        return DailyRhythmSummary(
            dateMillis = startOfDay,
            hasUsageAccess = true,
            totalScreenTimeMs = totalScreenTimeMs,
            totalPhoneFreeTimeMs = totalPhoneFreeTimeMs,
            longestBreakMs = longestBreakMs,
            breakCount = breaksOver15Min.size,
            nighttimeInactivity = nighttimeEstimate,
            inactivityEstimationStatus = nightStatus,
            topApps = topApps,
            sessions = todaySessions,
            inactivityPeriods = inactivityPeriods,
            rhythmScore = rhythmScore,
            stepCount = if (isHealthConnectConnected) healthConnectSteps else null,
            distanceMeters = if (isHealthConnectConnected) healthConnectDistanceMeters else null,
            activeMinutes = if (isHealthConnectConnected) healthConnectActiveMinutes else null,
            isRealData = true,
            rawEventsCount = rawEvents.size,
            lastEventTime = lastEventTimestamp
        )
    }

    private fun queryRawUsageEvents(startTime: Long, endTime: Long): List<RawEvent> {
        val result = mutableListOf<RawEvent>()
        if (usageStatsManager == null) return result

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName
                if (!pkg.isNullOrEmpty() && pkg != "android" && pkg != "com.android.systemui") {
                    result.add(
                        RawEvent(
                            packageName = pkg,
                            timestamp = event.timeStamp,
                            eventType = event.eventType
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked
        } catch (_: Exception) {
            // Defensive
        }

        return result.sortedBy { it.timestamp }
    }

    private fun queryDailyUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
        if (usageStatsManager == null) return emptyList()
        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            stats.filter { it.totalTimeInForeground > 0 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun convertRawEventsToSessions(
        events: List<RawEvent>,
        currentTime: Long
    ): List<UsageSession> {
        val sessions = mutableListOf<UsageSession>()
        if (events.isEmpty()) return sessions

        var currentPackage: String? = null
        var currentStartTime = 0L

        for (event in events) {
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // If a previous package was active, close it
                    if (currentPackage != null && currentStartTime > 0L) {
                        val duration = event.timestamp - currentStartTime
                        if (duration in 1000L..14400000L) { // between 1s and 4 hours
                            sessions.add(
                                UsageSession(
                                    id = UUID.randomUUID().toString(),
                                    packageName = currentPackage,
                                    appName = getAppLabel(currentPackage),
                                    startTime = currentStartTime,
                                    endTime = event.timestamp,
                                    durationMs = duration
                                )
                            )
                        }
                    }
                    currentPackage = event.packageName
                    currentStartTime = event.timestamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (currentPackage == event.packageName && currentStartTime > 0L) {
                        val duration = event.timestamp - currentStartTime
                        if (duration in 1000L..14400000L) {
                            sessions.add(
                                UsageSession(
                                    id = UUID.randomUUID().toString(),
                                    packageName = currentPackage,
                                    appName = getAppLabel(currentPackage),
                                    startTime = currentStartTime,
                                    endTime = event.timestamp,
                                    durationMs = duration
                                )
                            )
                        }
                        currentPackage = null
                        currentStartTime = 0L
                    }
                }
            }
        }

        // Close trailing active session if device is currently active
        if (currentPackage != null && currentStartTime > 0L) {
            val duration = (currentTime - currentStartTime).coerceIn(1000L, 30 * 60 * 1000L)
            sessions.add(
                UsageSession(
                    id = UUID.randomUUID().toString(),
                    packageName = currentPackage,
                    appName = getAppLabel(currentPackage),
                    startTime = currentStartTime,
                    endTime = currentStartTime + duration,
                    durationMs = duration
                )
            )
        }

        return sessions.sortedBy { it.startTime }
    }

    private fun calculateRealInactivityPeriods(
        sessions: List<UsageSession>,
        startOfDay: Long,
        now: Long
    ): List<InactivityPeriod> {
        val periods = mutableListOf<InactivityPeriod>()
        if (sessions.isEmpty()) {
            return periods
        }

        var lastSessionEnd = startOfDay
        for (session in sessions) {
            if (session.startTime > lastSessionEnd) {
                val gap = session.startTime - lastSessionEnd
                if (gap >= 60_000L) { // Stillness of at least 1 minute
                    periods.add(
                        InactivityPeriod(
                            startTime = lastSessionEnd,
                            endTime = session.startTime,
                            durationMs = gap
                        )
                    )
                }
            }
            lastSessionEnd = maxOf(lastSessionEnd, session.endTime)
        }

        if (lastSessionEnd < now) {
            val gap = now - lastSessionEnd
            if (gap >= 60_000L) {
                periods.add(
                    InactivityPeriod(
                        startTime = lastSessionEnd,
                        endTime = now,
                        durationMs = gap
                    )
                )
            }
        }

        return periods
    }

    private fun calculateRealNighttimeInactivity(
        allSessions: List<UsageSession>,
        windowStart: Long,
        windowEnd: Long
    ): Pair<NighttimeInactivityEstimate?, String> {
        val nightSessions = allSessions.filter {
            (it.startTime in windowStart..windowEnd) || (it.endTime in windowStart..windowEnd)
        }.sortedBy { it.startTime }

        if (nightSessions.isEmpty()) {
            return null to "No nighttime device sessions were recorded in the detection window."
        }

        var longestGapStart = 0L
        var longestGapEnd = 0L
        var maxGapDuration = 0L

        var lastAppName = ""
        var lastAppTime = 0L
        var firstAppName = ""
        var firstAppTime = 0L

        // 1. Gap from windowStart to first session
        val firstSession = nightSessions.first()
        val initialGap = firstSession.startTime - windowStart
        if (initialGap > maxGapDuration) {
            maxGapDuration = initialGap
            longestGapStart = windowStart
            longestGapEnd = firstSession.startTime
            lastAppName = "Previous Evening Downtime"
            lastAppTime = windowStart
            firstAppName = firstSession.appName
            firstAppTime = firstSession.startTime
        }

        // 2. Gaps between consecutive night sessions
        for (i in 0 until nightSessions.size - 1) {
            val cur = nightSessions[i]
            val next = nightSessions[i + 1]
            val gap = next.startTime - cur.endTime
            if (gap > maxGapDuration) {
                maxGapDuration = gap
                longestGapStart = cur.endTime
                longestGapEnd = next.startTime
                lastAppName = cur.appName
                lastAppTime = cur.endTime
                firstAppName = next.appName
                firstAppTime = next.startTime
            }
        }

        // 3. Gap from last session to windowEnd
        val lastSession = nightSessions.last()
        val trailingGap = windowEnd - lastSession.endTime
        if (trailingGap > maxGapDuration) {
            maxGapDuration = trailingGap
            longestGapStart = lastSession.endTime
            longestGapEnd = windowEnd
            lastAppName = lastSession.appName
            lastAppTime = lastSession.endTime
            firstAppName = "Morning Stillness"
            firstAppTime = windowEnd
        }

        // Rule: Must be at least 3 hours to be estimated as sleep/night stillness
        if (maxGapDuration < 3 * 3600 * 1000L) {
            return null to "Nighttime phone inactivity was under 3 hours; not enough stillness to estimate sleep."
        }

        val hours = maxGapDuration / (3600 * 1000.0)
        val hasClearBoundaries = lastAppTime != windowStart && firstAppTime != windowEnd

        val (confidence, reason) = when {
            hours >= 6.5 && hasClearBoundaries -> {
                ConfidenceLevel.HIGH to "Long unbroken nighttime inactivity interval (${String.format("%.1f", hours)}h) with activity right before and after."
            }
            hours >= 4.5 -> {
                ConfidenceLevel.MEDIUM to "Moderate nighttime inactivity interval (${String.format("%.1f", hours)}h)."
            }
            else -> {
                ConfidenceLevel.LOW to "Short or partial inactivity period (${String.format("%.1f", hours)}h); incomplete pattern."
            }
        }

        val estimate = NighttimeInactivityEstimate(
            startTime = longestGapStart,
            endTime = longestGapEnd,
            durationMs = maxGapDuration,
            lastActivityAppName = lastAppName,
            lastActivityTime = lastAppTime,
            firstActivityAppName = firstAppName,
            firstActivityTime = firstAppTime,
            confidence = confidence,
            confidenceReason = reason
        )

        return estimate to "Estimated from real phone activity. Inactivity does not confirm sleep."
    }

    private fun aggregateRealTopApps(
        sessions: List<UsageSession>,
        dailyStats: List<UsageStats>
    ): List<AppUsageStat> {
        val appMap = mutableMapOf<String, AppUsageStat>()

        // Add from sessions
        for (session in sessions) {
            val existing = appMap[session.packageName]
            val duration = (existing?.durationMs ?: 0L) + session.durationMs
            val launches = (existing?.launchCount ?: 0) + 1
            appMap[session.packageName] = AppUsageStat(
                packageName = session.packageName,
                appName = session.appName,
                durationMs = duration,
                launchCount = launches
            )
        }

        // Complement with UsageStats foreground time if greater
        for (stat in dailyStats) {
            val pkg = stat.packageName
            val existing = appMap[pkg]
            if (existing == null) {
                appMap[pkg] = AppUsageStat(
                    packageName = pkg,
                    appName = getAppLabel(pkg),
                    durationMs = stat.totalTimeInForeground,
                    launchCount = 1
                )
            } else if (stat.totalTimeInForeground > existing.durationMs) {
                appMap[pkg] = existing.copy(durationMs = stat.totalTimeInForeground)
            }
        }

        return appMap.values
            .filter { it.durationMs > 0 }
            .sortedByDescending { it.durationMs }
            .take(5)
    }

    private fun calculateRealRhythmScore(
        screenTimeMs: Long,
        phoneFreeMs: Long,
        breaksCount: Int,
        nighttimeEstimate: NighttimeInactivityEstimate?
    ): Int {
        var score = 50

        if (nighttimeEstimate != null) {
            val hours = nighttimeEstimate.durationMs / (3600 * 1000.0)
            if (hours in 6.5..8.5) {
                score += 25
            } else if (hours in 5.0..10.0) {
                score += 15
            }
        }

        if (breaksCount >= 3) {
            score += 15
        } else if (breaksCount >= 1) {
            score += 8
        }

        if (phoneFreeMs > screenTimeMs) {
            score += 10
        }

        return score.coerceIn(20, 98)
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
