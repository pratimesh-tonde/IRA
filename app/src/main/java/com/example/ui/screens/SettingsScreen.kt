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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import com.example.agent.AssistantLanguage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.HologramTheme
import com.example.ui.IraViewModel
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.HologramCyan
import com.example.ui.theme.HologramEmerald
import android.content.Intent
import com.example.agent.IraBackgroundService
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: IraViewModel,
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val themeColor = Color(currentTheme.primaryHex)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Column {
            Text(
                text = "SYSTEM CONFIGURATION",
                color = themeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Preferences & Diagnostics",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 0. Assistant Language Selector (Hindi, Marathi, English)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, themeColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = themeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assistant Language (संभाषण भाषा)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${selectedLanguage.nativeLabel} (${selectedLanguage.displayName})",
                            color = themeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantLanguage.entries.forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) themeColor.copy(alpha = 0.15f) else CosmicSurface)
                                .border(
                                    1.2.dp,
                                    if (isSelected) themeColor else themeColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setSelectedLanguage(lang) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lang.nativeLabel,
                                        color = if (isSelected) themeColor else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${lang.displayName}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = lang.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) themeColor else Color.Transparent)
                                    .border(1.5.dp, if (isSelected) themeColor else TextMuted, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = CosmicBlack,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Holographic Theme Selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Theme", tint = themeColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Hologram Theme Glow", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HologramTheme.values().forEach { theme ->
                        val isSelected = currentTheme == theme
                        val c = Color(theme.primaryHex)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setTheme(theme) }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(2.dp, if (isSelected) themeColor else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = theme.displayName.split(" ")[0], color = if (isSelected) TextPrimary else TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Voice Modulation Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = themeColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Neural TTS Voice Modulation", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Voice Pitch: ${(voicePitch * 10).toInt() / 10f}x", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = voicePitch,
                    onValueChange = { viewModel.setVoicePitch(it) },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = themeColor, activeTrackColor = themeColor)
                )

                Text(text = "Voice Speed: ${(voiceSpeed * 10).toInt() / 10f}x", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = voiceSpeed,
                    onValueChange = { viewModel.setVoiceSpeed(it) },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = themeColor, activeTrackColor = themeColor)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        viewModel.speechManager.speak(
                            "नमस्ते, मैं आईरा हूँ। मी इरा आहे। I am IRA, your offline on-device agentic assistant.",
                            "hi",
                            voicePitch,
                            voiceSpeed
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Multilingual Audio Output", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Privacy & Offline Architecture Certificate
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, HologramEmerald.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = HologramEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Zero Cloud Leak Architecture", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                val guarantees = listOf(
                    "100% On-Device Reasoning & Plan-Act-Observe-Reflect Loop",
                    "3-Tier Room Database Persistence (Episodic, Semantic, Procedural)",
                    "Multilingual Speech Engine (Hindi, Marathi, English, Hinglish)",
                    "Sandboxed Tool Registry with High-Risk Confirmation Gate",
                    "Transient Microphone Buffer (Zero Disk Audio Logging)"
                )

                guarantees.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text(text = "✔", color = HologramEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = item, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Background Execution & Word to Wake (Lock Mode)
        val context = LocalContext.current
        val isBgRunning by IraBackgroundService.isRunning.collectAsStateWithLifecycle()
        val isWakeWordActive by IraBackgroundService.isWakeWordActive.collectAsStateWithLifecycle()
        val lastWakeEvent by IraBackgroundService.lastWakeEvent.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .border(1.dp, if (isBgRunning) HologramEmerald.copy(alpha = 0.4f) else themeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Background",
                            tint = if (isBgRunning) HologramEmerald else themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Persistent Foreground Service",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBgRunning) "Active • Partial WakeLock & Service Online" else "Standby • Tap to activate background engine",
                                color = if (isBgRunning) HologramEmerald else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isBgRunning,
                        onCheckedChange = { start ->
                            if (start) {
                                IraBackgroundService.start(context)
                            } else {
                                IraBackgroundService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HologramEmerald,
                            checkedTrackColor = HologramEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = CosmicSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Word to Wake Sub-module
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, if (isWakeWordActive) themeColor.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(12.dp))
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
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Word to Wake",
                                    tint = if (isWakeWordActive) HologramCyan else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Word to Wake in Lock Mode",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isWakeWordActive) "Listening for 'Hey IRA' / 'आईरा' on lock screen" else "Muted",
                                        color = if (isWakeWordActive) HologramCyan else TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isWakeWordActive,
                                onCheckedChange = {
                                    IraBackgroundService.toggleWakeWord(context)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HologramCyan,
                                    checkedTrackColor = HologramCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = CosmicBlack
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Triggers: 'Hey IRA', 'IRA', 'Hello IRA', 'आईरा', 'इरा', 'हे इरा', 'सुनो आईरा'. Turns on the display, unlocks/shows over the lock screen, and executes commands automatically.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val serviceIntent = Intent(context, IraBackgroundService::class.java).apply {
                                        action = IraBackgroundService.ACTION_WAKE_AND_LISTEN
                                    }
                                    context.startService(serviceIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Word to Wake", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Status: $lastWakeEvent",
                            color = HologramEmerald,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. System Specs & About
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicSurface)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "IRA SYSTEM TELEMETRY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Assistant: IRA (Intelligent Reactive Agent) v2.0", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Lead Developer: Pratimesh Tonde", color = HologramCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = "Company: AIX TECH", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                Text(text = "Language Engine: Dynamic Mirroring (Speaks In User's Language)", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Automation: All Mobile Tasks & Apps (100% Offline)", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Orchestrator: PAOR Closed Reasoning Loop", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Memory Engine: 3-Tier SQLite/Room Vector-Graph", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "© AIX TECH • Developed by Pratimesh Tonde", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
