package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.ExecutionPlan
import com.example.agent.StepExecutionStatus
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.HologramAmber
import com.example.ui.theme.HologramCrimson
import com.example.ui.theme.HologramCyan
import com.example.ui.theme.HologramEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ThinkingProcessCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currentPlan: ExecutionPlan?,
    thinkingLogs: List<String>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x381E293B))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(HologramCyan.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HologramCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Agent Thinking",
                            tint = HologramCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PAOR Reasoning Stream",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (currentPlan != null) {
                                "${currentPlan.subAgent.displayName} • ${currentPlan.steps.size} Steps"
                            } else {
                                "Idle • Awaiting Directive"
                            },
                            color = HologramCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Multi-Step Stepper (if plan exists)
                    if (currentPlan != null && currentPlan.steps.isNotEmpty()) {
                        Text(
                            text = "PLAN STEPS",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentPlan.steps.forEachIndexed { idx, step ->
                                val statusColor = when (step.status) {
                                    StepExecutionStatus.SUCCESS -> HologramEmerald
                                    StepExecutionStatus.RUNNING -> HologramAmber
                                    StepExecutionStatus.FAILED -> HologramCrimson
                                    StepExecutionStatus.PENDING -> TextMuted
                                    StepExecutionStatus.SKIPPED -> TextSecondary
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CosmicSurface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (step.status) {
                                            StepExecutionStatus.SUCCESS -> Icons.Default.CheckCircle
                                            StepExecutionStatus.RUNNING -> Icons.Default.HourglassBottom
                                            StepExecutionStatus.FAILED -> Icons.Default.Warning
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = step.status.name,
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${idx + 1}. ${step.title}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = step.toolName,
                                        color = HologramCyan.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Live Thinking Telemetry Terminal
                    Text(
                        text = "COGNITIVE LOGS",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface)
                            .border(1.dp, HologramCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        if (thinkingLogs.isEmpty()) {
                            Text(
                                text = "Standby. IRA cognitive orchestrator ready on-device.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(thinkingLogs) { log ->
                                    val logColor = when {
                                        log.contains("[OBSERVE]") -> HologramEmerald
                                        log.contains("[CONFIRMATION GATE]") -> HologramAmber
                                        log.contains("[REFLECT]") -> HologramCrimson
                                        log.contains("[PLANNER]") -> HologramCyan
                                        else -> TextSecondary
                                    }
                                    Text(
                                        text = log,
                                        color = logColor,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
