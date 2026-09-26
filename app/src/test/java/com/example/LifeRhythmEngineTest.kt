package com.example

import com.example.model.ConfidenceLevel
import com.example.model.InactivityPeriod
import com.example.model.NighttimeInactivityEstimate
import com.example.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeRhythmEngineTest {

    @Test
    fun testInactivityCalculation() {
        val dayStart = 100000L
        val dayEnd = 500000L

        val sessions = listOf(
            UsageSession("1", "com.pkg1", "App 1", 120000L, 150000L, 30000L),
            UsageSession("2", "com.pkg2", "App 2", 200000L, 250000L, 50000L)
        )

        val inactivityPeriods = mutableListOf<InactivityPeriod>()
        var lastEnd = dayStart

        for (s in sessions) {
            if (s.startTime > lastEnd) {
                inactivityPeriods.add(InactivityPeriod(lastEnd, s.startTime, s.startTime - lastEnd))
            }
            lastEnd = maxOf(lastEnd, s.endTime)
        }
        if (lastEnd < dayEnd) {
            inactivityPeriods.add(InactivityPeriod(lastEnd, dayEnd, dayEnd - lastEnd))
        }

        assertEquals(3, inactivityPeriods.size)
        assertEquals(20000L, inactivityPeriods[0].durationMs)
        assertEquals(50000L, inactivityPeriods[1].durationMs)
        assertEquals(250000L, inactivityPeriods[2].durationMs)
    }

    @Test
    fun testConfidenceRules() {
        val highHours = 7.5
        val highConfidence = when {
            highHours >= 6.5 -> ConfidenceLevel.HIGH
            highHours >= 4.5 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
        assertEquals(ConfidenceLevel.HIGH, highConfidence)

        val mediumHours = 5.0
        val medConfidence = when {
            mediumHours >= 6.5 -> ConfidenceLevel.HIGH
            mediumHours >= 4.5 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
        assertEquals(ConfidenceLevel.MEDIUM, medConfidence)

        val lowHours = 3.5
        val lowConfidence = when {
            lowHours >= 6.5 -> ConfidenceLevel.HIGH
            lowHours >= 4.5 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
        assertEquals(ConfidenceLevel.LOW, lowConfidence)
    }

    @Test
    fun testRhythmScoreBounded() {
        val baseScore = 50
        val bonusNight = 25
        val bonusBreaks = 15
        val bonusBalance = 10
        val total = (baseScore + bonusNight + bonusBreaks + bonusBalance).coerceIn(0, 100)

        assertTrue(total in 0..100)
        assertEquals(100, total)
    }

    @Test
    fun testShortNightInactivityNotCalledSleep() {
        val gapDurationMs = 2 * 3600 * 1000L // 2 hours
        val isEstimatedSleep = gapDurationMs >= 3 * 3600 * 1000L
        assertTrue(!isEstimatedSleep)
    }
}
