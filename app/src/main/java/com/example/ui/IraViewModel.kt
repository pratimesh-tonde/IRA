package com.example.ui

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AssistantLanguage
import com.example.agent.AgentOrchestrator
import com.example.agent.AgentStatus
import com.example.agent.ConfirmationRequest
import com.example.agent.ExecutionPlan
import com.example.agent.SubAgentType
import com.example.audio.OfflineSpeechManager
import com.example.data.db.IraDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.EpisodicMemoryEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ProceduralRoutineEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.SemanticFactEntity
import com.example.data.repository.IraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    AGENT_CORE("IRA Core"),
    MEMORY_VAULT("3-Tier Memory"),
    TOOL_DECK("Tool Sandbox"),
    AUDIT_LOGS("Safety & Audit"),
    SETTINGS("Config")
}

enum class HologramTheme(val displayName: String, val primaryHex: Long) {
    IRIS_BALANCE("Geometric Iris", 0xFF6750A4),
    AMETHYST_CORE("Amethyst Rose", 0xFF7D5260),
    SLATE_BALANCE("Slate Violet", 0xFF625B71),
    EMERALD_HARMONY("Emerald Harmony", 0xFF1E7E34),
    CYAN_QUANTUM("Cerulean Balance", 0xFF00639B)
}

class IraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = IraDatabase.getDatabase(application, viewModelScope)
    val repository = IraRepository(database.memoryDao(), database.productivityDao())
    val speechManager = OfflineSpeechManager(application, viewModelScope)
    val orchestrator = AgentOrchestrator(application, repository, speechManager, viewModelScope)

    // UI Tab Navigation
    private val _currentTab = MutableStateFlow(AppTab.AGENT_CORE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Agent States from Orchestrator
    val agentStatus: StateFlow<AgentStatus> = orchestrator.status
    val activeSubAgent: StateFlow<SubAgentType> = orchestrator.activeSubAgent
    val currentPlan: StateFlow<ExecutionPlan?> = orchestrator.currentPlan
    val currentThinkingLog: StateFlow<List<String>> = orchestrator.currentThinkingLog
    val latestResponse: StateFlow<String> = orchestrator.latestResponse
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = orchestrator.pendingConfirmation

    // Audio & Waveform
    val audioAmplitude: StateFlow<Float> = speechManager.audioAmplitude
    val partialTranscript: StateFlow<String> = speechManager.partialTranscript

    // Reactive DB Data
    val recentMemories: StateFlow<List<EpisodicMemoryEntity>> = repository.recentMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFacts: StateFlow<List<SemanticFactEntity>> = repository.allFacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutines: StateFlow<List<ProceduralRoutineEntity>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Telemetry & Sensors
    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val prefs = application.getSharedPreferences("ira_system_prefs", Context.MODE_PRIVATE)

    // Language Preference (Hindi, Marathi, English)
    private val _selectedLanguage = MutableStateFlow(
        AssistantLanguage.fromCode(prefs.getString("assistant_language", "hi"))
    )
    val selectedLanguage: StateFlow<AssistantLanguage> = _selectedLanguage.asStateFlow()

    // Preferences & Theme
    private val _currentTheme = MutableStateFlow(HologramTheme.IRIS_BALANCE)
    val currentTheme: StateFlow<HologramTheme> = _currentTheme.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.0f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(1.0f)
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _isThinkingPanelExpanded = MutableStateFlow(true)
    val isThinkingPanelExpanded: StateFlow<Boolean> = _isThinkingPanelExpanded.asStateFlow()

    init {
        refreshBatterySensor()
        orchestrator.setLanguage(_selectedLanguage.value)
    }

    fun setSelectedLanguage(language: AssistantLanguage) {
        _selectedLanguage.value = language
        prefs.edit().putString("assistant_language", language.code).apply()
        orchestrator.setLanguage(language)
        viewModelScope.launch {
            repository.saveFact("User Preference", "assistant_language", language.displayName)
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun toggleThinkingPanel() {
        _isThinkingPanelExpanded.value = !_isThinkingPanelExpanded.value
    }

    fun setTheme(theme: HologramTheme) {
        _currentTheme.value = theme
    }

    fun setVoicePitch(pitch: Float) {
        _voicePitch.value = pitch
    }

    fun setVoiceSpeed(speed: Float) {
        _voiceSpeed.value = speed
    }

    fun sendPrompt(prompt: String) {
        orchestrator.handleUserInput(prompt)
    }

    fun toggleListening() {
        orchestrator.triggerListening()
    }

    fun interruptAgent() {
        orchestrator.interruptSpeech()
    }

    fun refreshBatterySensor() {
        val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        _batteryLevel.value = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        _isCharging.value = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
    }

    // Direct Database Actions for User Management
    fun addFact(category: String, key: String, value: String) {
        viewModelScope.launch {
            repository.saveFact(category, key, value)
        }
    }

    fun deleteFact(id: Long) {
        viewModelScope.launch {
            repository.deleteFact(id)
        }
    }

    fun addNote(title: String, content: String, category: String = "General") {
        viewModelScope.launch {
            repository.createNote(title, content, category)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun addReminder(title: String, time: String) {
        viewModelScope.launch {
            repository.addReminder(title, time)
        }
    }

    fun toggleReminder(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.completeReminder(id, completed)
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
        }
    }

    fun clearEpisodicMemory() {
        viewModelScope.launch {
            repository.clearEpisodicMemories()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
