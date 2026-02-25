package com.tishukoff.aiadventchallengeapp.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tishukoff.core.designsystem.AiAdventChallengeAppTheme
import com.tishukoff.feature.agent.api.TokenStats
import java.util.Locale

@Composable
fun TokenStatsPanel(
    stats: TokenStats,
    modifier: Modifier = Modifier,
) {
    if (stats.requestCount == 0) return

    var expanded by remember { mutableStateOf(false) }

    val contextColor by animateColorAsState(
        targetValue = when {
            stats.isOverLimit -> MaterialTheme.colorScheme.error
            stats.isNearLimit -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "contextColor",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tokens: ${formatNumber(stats.totalTokens)} | Cost: $${formatCost(stats.totalCostUsd)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "ctx: ${formatPercent(stats.contextUsagePercent)}%",
                style = MaterialTheme.typography.labelMedium,
                color = contextColor,
            )
        }

        LinearProgressIndicator(
            progress = { (stats.contextUsagePercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = contextColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        if (stats.isNearLimit) {
            val warningText = if (stats.isOverLimit) {
                "Context limit exceeded! Responses may be degraded."
            } else {
                "Approaching context limit. Consider starting a new chat."
            }
            Text(
                text = warningText,
                style = MaterialTheme.typography.labelSmall,
                color = contextColor,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                StatsRow("Requests", stats.requestCount.toString())
                StatsRow("Total input", formatNumber(stats.totalInputTokens))
                StatsRow("Total output", formatNumber(stats.totalOutputTokens))
                StatsRow("Avg input/req", formatNumber(stats.averageInputPerRequest))
                StatsRow("Avg output/req", formatNumber(stats.averageOutputPerRequest))
                StatsRow("Last input", formatNumber(stats.lastRequestInputTokens))
                StatsRow("Last output", formatNumber(stats.lastRequestOutputTokens))
                StatsRow("Last cost", "$${formatCost(stats.lastRequestCostUsd)}")
                StatsRow("Context window", formatNumber(stats.contextWindow))
            }
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatNumber(n: Int): String = String.format(Locale.US, "%,d", n)

private fun formatCost(cost: Double): String = String.format(Locale.US, "%.4f", cost)

private fun formatPercent(percent: Float): String = String.format(Locale.US, "%.1f", percent)

@Preview(showBackground = true)
@Composable
private fun TokenStatsPanelPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        TokenStatsPanel(
            stats = TokenStats(
                totalInputTokens = 1250,
                totalOutputTokens = 3400,
                totalCostUsd = 0.0548,
                requestCount = 5,
                contextWindow = 200_000,
                lastRequestInputTokens = 800,
                lastRequestOutputTokens = 920,
                lastRequestCostUsd = 0.0162,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TokenStatsPanelNearLimitPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        TokenStatsPanel(
            stats = TokenStats(
                totalInputTokens = 450_000,
                totalOutputTokens = 120_000,
                totalCostUsd = 3.15,
                requestCount = 42,
                contextWindow = 200_000,
                lastRequestInputTokens = 170_000,
                lastRequestOutputTokens = 1200,
                lastRequestCostUsd = 0.528,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TokenStatsPanelOverLimitPreview() {
    AiAdventChallengeAppTheme(dynamicColor = false) {
        TokenStatsPanel(
            stats = TokenStats(
                totalInputTokens = 800_000,
                totalOutputTokens = 200_000,
                totalCostUsd = 6.0,
                requestCount = 80,
                contextWindow = 200_000,
                lastRequestInputTokens = 198_000,
                lastRequestOutputTokens = 500,
                lastRequestCostUsd = 0.6015,
            ),
        )
    }
}
