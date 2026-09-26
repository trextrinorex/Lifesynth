package com.example.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyRhythmSummary
import com.example.model.InactivityPeriod
import com.example.model.UsageSession
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.Rhythm24hTimelineBar
import com.example.ui.components.formatDurationHoursMins
import com.example.ui.components.formatTime
import com.example.ui.theme.CircadianLavender
import com.example.ui.theme.InactivityMoon
import com.example.ui.theme.RestorativeSage

enum class TimelineFilter { ALL, ACTIVE_SESSIONS, PHONE_FREE, NIGHT_INACTIVITY }

sealed class TimelineItem {
    data class NightInactivity(
        val startTime: Long,
        val endTime: Long,
        val durationMs: Long,
        val lastApp: String,
        val firstApp: String,
        val confidence: com.example.model.ConfidenceLevel
    ) : TimelineItem()

    data class Session(val session: UsageSession) : TimelineItem()
    data class PhoneFree(val period: InactivityPeriod) : TimelineItem()
}

@Composable
fun TimelineScreen(
    summary: DailyRhythmSummary?,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(TimelineFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("timeline_screen")
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Where Did My Day Go?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Chronological stream of active sessions and restorative pauses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (summary != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Rhythm24hTimelineBar(summary = summary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == TimelineFilter.ALL,
                        onClick = { selectedFilter = TimelineFilter.ALL },
                        label = { Text("All Rhythms") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TimelineFilter.ACTIVE_SESSIONS,
                        onClick = { selectedFilter = TimelineFilter.ACTIVE_SESSIONS },
                        label = { Text("Active Screen") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TimelineFilter.PHONE_FREE,
                        onClick = { selectedFilter = TimelineFilter.PHONE_FREE },
                        label = { Text("Phone-Free Breaks") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TimelineFilter.NIGHT_INACTIVITY,
                        onClick = { selectedFilter = TimelineFilter.NIGHT_INACTIVITY },
                        label = { Text("Night Inactivity") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Combine items chronologically
            val items = remember(summary, selectedFilter) {
                val list = mutableListOf<TimelineItem>()

                // Night inactivity
                summary.nighttimeInactivity?.let { night ->
                    if (selectedFilter == TimelineFilter.ALL || selectedFilter == TimelineFilter.NIGHT_INACTIVITY) {
                        list.add(
                            TimelineItem.NightInactivity(
                                startTime = night.startTime,
                                endTime = night.endTime,
                                durationMs = night.durationMs,
                                lastApp = night.lastActivityAppName,
                                firstApp = night.firstActivityAppName,
                                confidence = night.confidence
                            )
                        )
                    }
                }

                // Active sessions
                if (selectedFilter == TimelineFilter.ALL || selectedFilter == TimelineFilter.ACTIVE_SESSIONS) {
                    summary.sessions.forEach { list.add(TimelineItem.Session(it)) }
                }

                // Phone free periods (>= 10 min)
                if (selectedFilter == TimelineFilter.ALL || selectedFilter == TimelineFilter.PHONE_FREE) {
                    summary.inactivityPeriods.filter { it.durationMs >= 10 * 60 * 1000L }
                        .forEach { list.add(TimelineItem.PhoneFree(it)) }
                }

                list.sortedBy { item ->
                    when (item) {
                        is TimelineItem.NightInactivity -> item.startTime
                        is TimelineItem.Session -> item.session.startTime
                        is TimelineItem.PhoneFree -> item.period.startTime
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(items) { item ->
                    when (item) {
                        is TimelineItem.NightInactivity -> NightInactivityRow(item)
                        is TimelineItem.Session -> SessionRow(item.session)
                        is TimelineItem.PhoneFree -> PhoneFreeRow(item.period)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MedicalDisclaimerCard()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No timeline events recorded yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NightInactivityRow(item: TimelineItem.NightInactivity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(InactivityMoon),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Night Inactivity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ConfidenceBadge(confidence = item.confidence)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${formatTime(item.startTime)} – ${formatTime(item.endTime)} • ${formatDurationHoursMins(item.durationMs)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Boundaries: Last '${item.lastApp}' → First '${item.firstApp}'",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: UsageSession) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CircadianLavender.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = CircadianLavender,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = session.appName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDurationHoursMins(session.durationMs),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${formatTime(session.startTime)} – ${formatTime(session.endTime)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
private fun PhoneFreeRow(period: InactivityPeriod) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(RestorativeSage.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = RestorativeSage,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Phone-Free Pause",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDurationHoursMins(period.durationMs),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = RestorativeSage
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${formatTime(period.startTime)} – ${formatTime(period.endTime)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}
