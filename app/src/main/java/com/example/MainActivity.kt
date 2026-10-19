package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import com.example.agent.IraBackgroundService
import com.example.ui.AppTab
import com.example.ui.IraViewModel
import com.example.ui.screens.AuditSafetyScreen
import com.example.ui.screens.MainAgentScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolDeckScreen
import com.example.ui.theme.GeoCanvas
import com.example.ui.theme.GeoOutlineVariant
import com.example.ui.theme.GeoPrimaryContainer
import com.example.ui.theme.GeoSurface
import com.example.ui.theme.GeoTextPrimary
import com.example.ui.theme.GeoTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: IraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContent {
            MyApplicationTheme {
                val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    IraBackgroundService.start(this@MainActivity)
                }

                LaunchedEffect(Unit) {
                    val permissionsNeeded = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val ungranted = permissionsNeeded.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (ungranted.isNotEmpty()) {
                        multiplePermissionsLauncher.launch(ungranted.toTypedArray())
                    } else {
                        IraBackgroundService.start(this@MainActivity)
                    }

                    handleWakeIntent(intent)
                }

                IraAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        val isWake = intent?.getBooleanExtra(IraBackgroundService.EXTRA_WAKE_WORD_ACTIVATED, false) ?: false
        if (isWake) {
            val command = intent?.getStringExtra(IraBackgroundService.EXTRA_WAKE_COMMAND)
            if (!command.isNullOrBlank()) {
                viewModel.sendPrompt(command)
            } else {
                viewModel.toggleListening()
            }
        }
    }
}

@Composable
fun IraAppRoot(viewModel: IraViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val themeColor = Color(currentTheme.primaryHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(GeoCanvas)
                // Ambient Aurora Orbs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(themeColor.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.width * 0.75f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.45f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.85f),
                        radius = size.width * 0.7f
                    )
                )
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                // Floating Frosted Glass Capsule Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xCC0D131F))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val navItems = listOf(
                            Triple(AppTab.AGENT_CORE, "Core", Icons.Default.Psychology),
                            Triple(AppTab.MEMORY_VAULT, "Memory", Icons.Default.Storage),
                            Triple(AppTab.TOOL_DECK, "Tools", Icons.Default.Build),
                            Triple(AppTab.AUDIT_LOGS, "Audit", Icons.Default.Security),
                            Triple(AppTab.SETTINGS, "Config", Icons.Default.Settings)
                        )

                        navItems.forEach { (tab, label, icon) ->
                            val isSelected = currentTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) themeColor.copy(alpha = 0.22f) else Color.Transparent
                                    )
                                    .clickable { viewModel.setTab(tab) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) themeColor else GeoTextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) GeoTextPrimary else GeoTextSecondary.copy(alpha = 0.6f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppTab.AGENT_CORE -> MainAgentScreen(viewModel = viewModel)
                    AppTab.MEMORY_VAULT -> MemoryVaultScreen(viewModel = viewModel)
                    AppTab.TOOL_DECK -> ToolDeckScreen(viewModel = viewModel)
                    AppTab.AUDIT_LOGS -> AuditSafetyScreen(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
