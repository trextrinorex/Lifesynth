package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfidenceLevel
import com.example.model.DailyRhythmSummary
import com.example.model.UsageSession
import com.example.ui.theme.CircadianLavender
import com.example.ui.theme.InactivityMoon
import com.example.ui.theme.MintTag
import com.example.ui.theme.OnMintTag
import com.example.ui.theme.RestorativeSage
import com.example.ui.theme.SubduedTerracotta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicalDisclaimerCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("disclaimer_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notice",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estimated from phone activity. Inactivity does not confirm sleep.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "LifeRhythm calculates behavioral stillness intervals using device usage timers. Data is for personal lifestyle reflection and does not constitute a medical diagnosis.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ConfidenceBadge(
    confidence: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (confidence) {
        ConfidenceLevel.HIGH -> Triple(MintTag, OnMintTag, "High Confidence")
        ConfidenceLevel.MEDIUM -> Triple(Color(0xFFE2E3FF), Color(0xFF2B2E60), "Medium Confidence")
        ConfidenceLevel.LOW -> Triple(Color(0xFFFFE0DB), Color(0xFF7A271B), "Low Confidence")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
fun Rhythm24hTimelineBar(
    summary: DailyRhythmSummary,
    modifier: Modifier = Modifier,
    onSegmentClick: ((String) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "00:00",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
            Text(
                text = "06:00",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
            Text(
                text = "12:00",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
            Text(
                text = "18:00",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
            Text(
                text = "24:00",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Visual 24h bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            // Render estimated night stillness block
            summary.nighttimeInactivity?.let { night ->
                val dayStart = summary.dateMillis
                val dayEnd = dayStart + 24 * 3600 * 1000L

                val normStart = ((night.startTime - dayStart).toFloat() / (24 * 3600 * 1000L)).coerceIn(0f, 1f)
                val normEnd = ((night.endTime - dayStart).toFloat() / (24 * 3600 * 1000L)).coerceIn(0f, 1f)
                val widthFraction = (normEnd - normStart).coerceAtLeast(0.04f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = normEnd)
                        .height(28.dp)
                        .padding(start = (normStart * 300).dp) // approximate visual placement
                        .clip(RoundedCornerShape(4.dp))
                        .background(InactivityMoon.copy(alpha = 0.85f))
                )
            }

            // Render daytime active sessions
            Row(
                modifier = Modifier.fillMaxWidth().height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val totalMs = 24 * 3600 * 1000f
                summary.sessions.take(12).forEach { session ->
                    val weight = (session.durationMs / totalMs * 24f).coerceAtLeast(0.02f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CircadianLavender)
                            .clickable { onSegmentClick?.invoke(session.appName) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = CircadianLavender, label = "Active Screen")
            LegendItem(color = InactivityMoon, label = "Night Inactivity")
            LegendItem(color = MaterialTheme.colorScheme.surfaceContainerHigh, label = "Phone-Free Break")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

fun formatTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatDurationHoursMins(durationMs: Long): String {
    val totalMinutes = durationMs / (60 * 1000)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
