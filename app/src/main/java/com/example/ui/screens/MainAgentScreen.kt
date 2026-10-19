package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.agent.AgentStatus
import com.example.agent.AssistantLanguage
import com.example.ui.IraViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.HolographicOrb
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.GeoOutline
import com.example.ui.theme.GeoOutlineVariant
import com.example.ui.theme.GeoSuccess
import com.example.ui.theme.GeoSuccessContainer
import com.example.ui.theme.GeoSurface
import com.example.ui.theme.GeoSurfaceVariant
import com.example.ui.theme.GeoTextMuted
import com.example.ui.theme.GeoTextPrimary
import com.example.ui.theme.GeoTextSecondary
import com.example.ui.theme.HologramAmber
import com.example.ui.theme.HologramCrimson
import com.example.ui.theme.HologramCyan
import com.example.ui.theme.HologramEmerald
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainAgentScreen(
    viewModel: IraViewModel,
    modifier: Modifier = Modifier
) {
    val agentStatus by viewModel.agentStatus.collectAsStateWithLifecycle()
    val activeSubAgent by viewModel.activeSubAgent.collectAsStateWithLifecycle()
    val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val currentPlan by viewModel.currentPlan.collectAsStateWithLifecycle()
    val thinkingLogs by viewModel.currentThinkingLog.collectAsStateWithLifecycle()
    val latestResponse by viewModel.latestResponse.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val isThinkingExpanded by viewModel.isThinkingPanelExpanded.collectAsStateWithLifecycle()
    val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var textInput by remember { mutableStateOf("") }
    val themeColor = Color(currentTheme.primaryHex)

    // Security confirmation gate modal
    pendingConfirmation?.let { req ->
        ConfirmationDialog(request = req)
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    var allPermissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allPermissionsGranted = results.values.all { it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Minimal Glass Telemetry Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x331E293B))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.04f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // IRA Identity & Active Sub-Agent
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (agentStatus) {
                                AgentStatus.IDLE -> HologramCyan
                                AgentStatus.LISTENING -> HologramEmerald
                                AgentStatus.THINKING -> HologramViolet
                                AgentStatus.SPEAKING -> HologramCyan
                                AgentStatus.CONFIRMING -> HologramAmber
                                AgentStatus.EXECUTING -> HologramEmerald
                                AgentStatus.ERROR -> HologramCrimson
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IRA",
                    color = GeoTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.20f))
                        .border(1.dp, themeColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = activeSubAgent.displayName,
                        color = themeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Real-Time Sensor Telemetry: Offline Badge & Battery
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GeoSuccessContainer)
                        .border(1.dp, GeoSuccess.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "100% OFFLINE",
                        color = GeoSuccess,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = "Battery Status",
                    tint = if (batteryLevel < 20) HologramCrimson else themeColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryLevel%",
                    color = GeoTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Permissions Status Banner (shown only when required for minimal clarity)
        if (!allPermissionsGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x2BFBBF24))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(HologramAmber.copy(alpha = 0.6f), HologramAmber.copy(alpha = 0.15f))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permissions",
                        tint = HologramAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Capabilities (Mic, Calls, GPS, SMS)",
                        color = HologramAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HologramAmber)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "GRANT",
                        color = Color(0xFF090D16),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // UI Language Selector: Minimal Glass Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x2B1E293B))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, end = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = themeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VOICE",
                        color = themeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            AssistantLanguage.entries.forEach { lang ->
                val isSelected = selectedLanguage == lang
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) themeColor.copy(alpha = 0.28f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) themeColor.copy(alpha = 0.65f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.setSelectedLanguage(lang) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${lang.nativeLabel} (${lang.displayName})",
                            color = if (isSelected) GeoTextPrimary else GeoTextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Central Holographic Reactive Glass Orb
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HolographicOrb(
                status = agentStatus,
                audioAmplitude = audioAmplitude,
                baseColor = themeColor,
                onClick = { viewModel.toggleListening() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State Pill & Transcript
        val statusLabel = when (agentStatus) {
            AgentStatus.IDLE -> "IRA READY • TAP ORB OR MIC TO SPEAK"
            AgentStatus.LISTENING -> "LISTENING TO VOICE..."
            AgentStatus.THINKING -> "PROCESSING COMMAND..."
            AgentStatus.SPEAKING -> "IRA SPEAKING"
            AgentStatus.CONFIRMING -> "SECURITY AUTHORIZATION"
            AgentStatus.EXECUTING -> "EXECUTING OPERATION..."
            AgentStatus.ERROR -> "SYSTEM ERROR"
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x3B1E293B))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.2f),
                            themeColor.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (agentStatus == AgentStatus.LISTENING || agentStatus == AgentStatus.SPEAKING) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Waveform",
                        tint = themeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = statusLabel,
                    color = themeColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Live Voice Transcript (when listening)
        if (agentStatus == AgentStatus.LISTENING && partialTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x331E293B))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "\"$partialTranscript\"",
                    color = GeoTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Output Display Card (Minimal Glass Panel)
        if (latestResponse.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x3F1E293B))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.14f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(themeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "IRA • ${selectedLanguage.nativeLabel} (${selectedLanguage.displayName.uppercase()})",
                                color = themeColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(latestResponse))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Response",
                                    tint = GeoTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(themeColor.copy(alpha = 0.22f))
                                    .clickable {
                                        viewModel.sendPrompt(latestResponse)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Replay Speech",
                                    tint = themeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = latestResponse,
                        color = GeoTextPrimary,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Quick Suggestion Chips (Minimal Frosted Glass Capsules)
        val suggestions = when (selectedLanguage) {
            AssistantLanguage.HINDI -> listOf(
                "नमस्ते इरा! आप कैसी हैं?",
                "लोकेशन बताओ (GPS)",
                "व्हाट्सएप चैट खोलो",
                "मुंबई का रास्ता दिखाओ",
                "कॉल करो Rahul",
                "कैमरा खोलो",
                "टॉर्च चालू करो",
                "गाना बजाओ",
                "बैटरी कितनी है?",
                "अलार्म लगाओ 07:00 AM",
                "नोट लिखो: ज़रूरी काम",
                "भारत की राजधानी क्या है?"
            )
            AssistantLanguage.MARATHI -> listOf(
                "नमस्कार इरा! कशी आहेस?",
                "माझे स्थान सांगा (GPS)",
                "व्हॉट्सॲप चॅट उघडा",
                "मुंबई नेव्हिगेशन सुरू करा",
                "फोन लावा Rahul",
                "कॅमेरा उघडा",
                "फ्लॅशलाइट सुरू करा",
                "गाणे लावा",
                "बॅटरी किती आहे?",
                "अलार्म लावा ०७:०० AM",
                "नोंद करा: महत्त्वाची मिटिंग",
                "भारताची राजधानी कोणती?"
            )
            AssistantLanguage.ENGLISH -> listOf(
                "Hello Ira! How are you?",
                "Tell my Location (GPS)",
                "Open WhatsApp Chat",
                "Navigate to Mumbai",
                "Call Rahul",
                "Open Camera",
                "Turn on flashlight",
                "Play Music",
                "Check battery status",
                "Set Alarm 07:00 AM",
                "Create note: Important Meeting",
                "Capital of India"
            )
        }

        val chipScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x261E293B))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.sendPrompt(prompt) }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = prompt,
                        color = GeoTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Input Console (Frosted Glass Capsule + Glowing Mic FAB)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        when (selectedLanguage) {
                            AssistantLanguage.HINDI -> "IRA को हिंदी में कमांड दें (उदा: टॉर्च चालू करो)..."
                            AssistantLanguage.MARATHI -> "IRA ला मराठीत आदेश द्या (उदा: फ्लॅशलाइट लावा)..."
                            AssistantLanguage.ENGLISH -> "Command IRA in English (e.g. Turn on flashlight)..."
                        },
                        color = GeoTextMuted,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x351E293B),
                    unfocusedContainerColor = Color(0x281E293B),
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedTextColor = GeoTextPrimary,
                    unfocusedTextColor = GeoTextPrimary,
                    cursorColor = themeColor
                ),
                singleLine = true,
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendPrompt(textInput)
                                textInput = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Command",
                                tint = themeColor
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Glowing Circular Mic Trigger
            FloatingActionButton(
                onClick = { viewModel.toggleListening() },
                containerColor = if (agentStatus == AgentStatus.LISTENING) HologramEmerald else themeColor,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier
                    .size(54.dp)
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (agentStatus == AgentStatus.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input Toggle",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Footer: Minimal attribution
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "© AIX TECH • Developed by Pratimesh Tonde",
                color = GeoTextMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        }
    }
}
