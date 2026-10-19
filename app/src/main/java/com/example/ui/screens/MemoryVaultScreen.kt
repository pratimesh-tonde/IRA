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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryVaultScreen(
    viewModel: IraViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTier by remember { mutableIntStateOf(0) }
    val tiers = listOf("Episodic (History)", "Semantic (Facts RAG)", "Procedural (Routines)", "Local Notes")

    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val themeColor = Color(currentTheme.primaryHex)

    val memories by viewModel.recentMemories.collectAsStateWithLifecycle()
    val facts by viewModel.allFacts.collectAsStateWithLifecycle()
    val routines by viewModel.allRoutines.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    var showAddFactDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("Preference") }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

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
                    text = "3-TIER MEMORY ENGINE",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "On-Device Neural Memory",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (selectedTier == 1) {
                Button(
                    onClick = { showAddFactDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Fact", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Fact", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else if (selectedTier == 0) {
                OutlinedButton(
                    onClick = { viewModel.clearEpisodicMemory() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HologramCrimson),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Logs", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tier Selection Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTier,
            containerColor = CosmicNavy,
            contentColor = themeColor,
            edgePadding = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tiers.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTier == index,
                    onClick = { selectedTier = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTier == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTier == index) themeColor else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content Area Based on Tier
        when (selectedTier) {
            0 -> {
                // Tier 1: Episodic Memories
                if (memories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No episodic interactions recorded yet.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(memories) { mem ->
                            val timeStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(mem.timestamp))
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
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "Episode",
                                                tint = themeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${mem.subAgent} • ${mem.language}",
                                                color = themeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(text = timeStr, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "User: \"${mem.userPrompt}\"",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "IRA: ${mem.iraResponse}",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tier 2: Semantic Memory Facts
                if (facts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No semantic facts stored.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(facts) { fact ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CosmicNavy)
                                    .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(themeColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = fact.category.uppercase(),
                                                color = themeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = fact.factKey,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = fact.factValue,
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteFact(fact.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Fact",
                                            tint = HologramCrimson.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tier 3: Procedural Routines
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(routines) { routine ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CosmicNavy)
                                .border(1.dp, HologramAmber.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Routine",
                                            tint = HologramAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = routine.triggerName,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HologramEmerald.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RUNS: ${routine.executionCount}",
                                            color = HologramEmerald,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Trigger: ${routine.conditionDesc}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CosmicSurface)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = routine.stepsJson,
                                        color = themeColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Local Notes List
                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes saved. Command IRA to take notes!", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(notes) { note ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CosmicNavy)
                                    .border(1.dp, themeColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = note.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = note.content, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Note",
                                            tint = HologramCrimson.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
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

    // Add Fact Modal Dialog
    if (showAddFactDialog) {
        AlertDialog(
            onDismissRequest = { showAddFactDialog = false },
            containerColor = CosmicNavy,
            title = {
                Text("Add Semantic Memory Fact", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category (e.g. User, Preference, Contact)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = themeColor.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Fact Key (e.g. favorite_color, brother_name)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = themeColor.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Fact Value (e.g. Blue, Rahul)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = themeColor.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            viewModel.addFact(newCategory, newKey, newValue)
                            newKey = ""
                            newValue = ""
                            showAddFactDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White)
                ) {
                    Text("Save to Memory")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddFactDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
