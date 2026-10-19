package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.RiskLevel
import com.example.agent.SubAgentType
import com.example.agent.ToolDefinition
import com.example.agent.ToolRegistry
import com.example.ui.IraViewModel
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.HologramAmber
import com.example.ui.theme.HologramCrimson
import com.example.ui.theme.HologramCyan
import com.example.ui.theme.HologramEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ToolDeckScreen(
    viewModel: IraViewModel,
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val themeColor = Color(currentTheme.primaryHex)

    val subAgentFilters = listOf("ALL", "COMMUNICATION", "PRODUCTIVITY", "DEVICE", "KNOWLEDGE")
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val allTools = ToolRegistry.tools.values.toList()
    val filteredTools = when (selectedFilterIndex) {
        1 -> allTools.filter { it.subAgent == SubAgentType.COMMUNICATION }
        2 -> allTools.filter { it.subAgent == SubAgentType.PRODUCTIVITY }
        3 -> allTools.filter { it.subAgent == SubAgentType.DEVICE }
        4 -> allTools.filter { it.subAgent == SubAgentType.KNOWLEDGE }
        else -> allTools
    }

    var selectedToolResult by remember { mutableStateOf<Pair<String, String>?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOOL REGISTRY SANDBOX (30+ ACTIONS)",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Agent Capabilities Deck",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${allTools.size} Tools Ready",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedFilterIndex,
            containerColor = CosmicNavy,
            contentColor = themeColor,
            edgePadding = 0.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, com.example.ui.theme.GeoOutlineVariant, RoundedCornerShape(12.dp))
        ) {
            subAgentFilters.forEachIndexed { index, name ->
                Tab(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    text = {
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFilterIndex == index) themeColor else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tool execution result feedback banner
        selectedToolResult?.let { (toolName, out) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicSurface)
                    .border(1.dp, HologramEmerald, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Direct Output ($toolName):", color = HologramEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Tap to dismiss", color = TextMuted, fontSize = 10.sp, modifier = Modifier.clickable { selectedToolResult = null })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = out, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tools List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredTools) { tool ->
                ToolCard(
                    tool = tool,
                    themeColor = themeColor,
                    onExecuteDirect = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val defaultArgs = when (tool.name) {
                                "evaluate_math" -> mapOf("expression" to "25 * 40 + 15")
                                "convert_units" -> mapOf("value" to "10", "from" to "km", "to" to "miles")
                                "query_knowledge_base" -> mapOf("query" to "speed of light")
                                "translate_text" -> mapOf("text" to "Hello, how are you?", "target_lang" to "hindi")
                                "create_note" -> mapOf("title" to "Sandbox Note", "content" to "Created via Tool Deck")
                                "toggle_flashlight" -> mapOf("state" to "toggle")
                                "adjust_volume" -> mapOf("stream" to "media", "direction" to "up")
                                else -> emptyMap()
                            }
                            val res = tool.execute(context, viewModel.repository, defaultArgs)
                            selectedToolResult = Pair(tool.name, res.outputMessage)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: ToolDefinition,
    themeColor: Color,
    onExecuteDirect: () -> Unit
) {
    val riskBadgeColor = when (tool.riskLevel) {
        RiskLevel.LOW -> HologramEmerald
        RiskLevel.MEDIUM -> HologramAmber
        RiskLevel.HIGH -> HologramCrimson
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(1.dp, themeColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Tool",
                        tint = themeColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tool.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskBadgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${tool.riskLevel} RISK",
                            color = riskBadgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onExecuteDirect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor.copy(alpha = 0.15f),
                            contentColor = themeColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Test", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tool.description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (tool.parameterDescriptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Parameters: " + tool.parameterDescriptions.keys.joinToString(", ") { "$it: String" },
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
