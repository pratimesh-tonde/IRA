package com.example.agent

import android.content.Context
import com.example.audio.OfflineSpeechManager
import com.example.data.repository.IraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentOrchestrator(
    private val context: Context,
    private val repository: IraRepository,
    private val speechManager: OfflineSpeechManager,
    private val scope: CoroutineScope
) {
    private val _status = MutableStateFlow(AgentStatus.IDLE)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()

    private val _activeSubAgent = MutableStateFlow(SubAgentType.KNOWLEDGE)
    val activeSubAgent: StateFlow<SubAgentType> = _activeSubAgent.asStateFlow()

    private val _targetLanguage = MutableStateFlow(AssistantLanguage.HINDI)
    val targetLanguage: StateFlow<AssistantLanguage> = _targetLanguage.asStateFlow()

    private val _currentPlan = MutableStateFlow<ExecutionPlan?>(null)
    val currentPlan: StateFlow<ExecutionPlan?> = _currentPlan.asStateFlow()

    private val _currentThinkingLog = MutableStateFlow<List<String>>(emptyList())
    val currentThinkingLog: StateFlow<List<String>> = _currentThinkingLog.asStateFlow()

    private val _latestResponse = MutableStateFlow("")
    val latestResponse: StateFlow<String> = _latestResponse.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    fun setLanguage(language: AssistantLanguage) {
        _targetLanguage.value = language
    }

    fun handleUserInput(input: String) {
        if (input.isBlank()) return
        scope.launch(Dispatchers.Default) {
            executePaorLoop(input)
        }
    }

    private suspend fun executePaorLoop(userGoal: String) {
        val activeLang = _targetLanguage.value
        _status.value = AgentStatus.THINKING
        _currentThinkingLog.value = listOf("• [PLANNER]: Analyzing goal '$userGoal'...")

        // Phase 1: PLAN with user selected language
        val planAnalysis = OfflineReasoningEngine.analyzeAndPlan(userGoal, activeLang.detectedLang)
        _activeSubAgent.value = planAnalysis.selectedSubAgent

        val logs = _currentThinkingLog.value.toMutableList()
        logs.add("• [PLANNER]: Output Language: ${activeLang.nativeLabel} (${activeLang.displayName})")
        logs.add("• [ROUTER]: Assigned to Sub-Agent: ${planAnalysis.selectedSubAgent.displayName}")
        logs.add("• [THOUGHT]: ${planAnalysis.initialThought}")
        _currentThinkingLog.value = logs

        val executionPlan = ExecutionPlan(
            goal = userGoal,
            subAgent = planAnalysis.selectedSubAgent,
            steps = planAnalysis.steps
        )
        _currentPlan.value = executionPlan

        val stepResults = mutableListOf<Pair<PlanStep, ToolResult>>()

        // Phase 2: ACT -> OBSERVE -> REFLECT for each step
        for ((index, step) in executionPlan.steps.withIndex()) {
            executionPlan.currentStepIndex = index
            step.status = StepExecutionStatus.RUNNING
            _status.value = AgentStatus.EXECUTING

            val stepLogs = _currentThinkingLog.value.toMutableList()
            stepLogs.add("• [ACT Step ${index + 1}/${executionPlan.steps.size}]: Executing '${step.title}' via tool '${step.toolName}' (Risk: ${step.riskLevel})")
            _currentThinkingLog.value = stepLogs

            // Risk-Tiered Confirmation Gate
            if (step.riskLevel == RiskLevel.HIGH) {
                _status.value = AgentStatus.CONFIRMING
                stepLogs.add("• [CONFIRMATION GATE]: Halting for user authorization on sensitive action...")
                _currentThinkingLog.value = stepLogs

                val confirmed = awaitUserConfirmation(step)
                if (!confirmed) {
                    step.status = StepExecutionStatus.SKIPPED
                    step.observation = "Action declined by user."
                    stepResults.add(Pair(step, ToolResult(false, "User rejected action authorization.")))
                    withContext(Dispatchers.IO) {
                        repository.logAudit(
                            step.toolName,
                            step.arguments.toString(),
                            step.riskLevel.name,
                            "REJECTED",
                            "User rejected confirmation gate."
                        )
                    }
                    continue
                }
            }

            // ACT: Execute tool
            _status.value = AgentStatus.EXECUTING
            val toolDef = ToolRegistry.tools[step.toolName]
            val result = if (toolDef != null) {
                withContext(Dispatchers.Main) {
                    toolDef.execute(context, repository, step.arguments)
                }
            } else {
                ToolResult(false, "Tool '${step.toolName}' not found in registry.")
            }

            // OBSERVE: Evaluate output
            if (result.isSuccess) {
                step.status = StepExecutionStatus.SUCCESS
                step.observation = result.outputMessage
                val obsLogs = _currentThinkingLog.value.toMutableList()
                obsLogs.add("• [OBSERVE]: Success -> ${result.outputMessage}")
                _currentThinkingLog.value = obsLogs
            } else {
                step.status = StepExecutionStatus.FAILED
                step.observation = result.outputMessage
                // REFLECT: Self-reflection on failure
                val refLogs = _currentThinkingLog.value.toMutableList()
                refLogs.add("• [REFLECT]: Tool execution failed! Reason: ${result.outputMessage}. Evaluating alternate fallback approach.")
                _currentThinkingLog.value = refLogs
            }

            stepResults.add(Pair(step, result))

            // Audit log persistence
            withContext(Dispatchers.IO) {
                repository.logAudit(
                    step.toolName,
                    step.arguments.toString(),
                    step.riskLevel.name,
                    if (result.isSuccess) "SUCCESS" else "FAILED",
                    result.outputMessage
                )
            }
        }

        executionPlan.isCompleted = true

        // Phase 4: FORMULATE & SPEAK in user selected language
        val (finalText, sentiment) = OfflineReasoningEngine.formulateFinalResponse(
            userGoal,
            activeLang.detectedLang,
            stepResults
        )

        _latestResponse.value = finalText
        _status.value = AgentStatus.SPEAKING

        val finalLogs = _currentThinkingLog.value.toMutableList()
        finalLogs.add("• [REFLECTOR]: Full PAOR loop finished. Speaking response in ${activeLang.nativeLabel} (${activeLang.displayName}).")
        _currentThinkingLog.value = finalLogs

        // Record episode into Room database
        withContext(Dispatchers.IO) {
            repository.recordEpisode(
                userPrompt = userGoal,
                iraResponse = finalText,
                subAgent = planAnalysis.selectedSubAgent.displayName,
                language = activeLang.displayName,
                sentiment = sentiment,
                planSummary = "${executionPlan.steps.size} step(s) executed"
            )
        }

        // Multilingual vocalization in selected language with calm, natural, respectful human cadence
        speechManager.speak(
            text = finalText,
            languageCode = activeLang.code,
            pitch = 1.0f,
            speechRate = 0.95f
        )

        // Return to IDLE once speaking concludes
        scope.launch {
            while (speechManager.isSpeaking.value) {
                kotlinx.coroutines.delay(100)
            }
            if (_status.value == AgentStatus.SPEAKING) {
                _status.value = AgentStatus.IDLE
            }
        }
    }

    private suspend fun awaitUserConfirmation(step: PlanStep): Boolean =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            _pendingConfirmation.value = ConfirmationRequest(
                toolName = step.toolName,
                title = step.title,
                description = "High-risk tool call '${step.toolName}' with parameters ${step.arguments}. Please confirm execution.",
                arguments = step.arguments,
                riskLevel = step.riskLevel,
                onConfirm = {
                    _pendingConfirmation.value = null
                    continuation.resumeWith(Result.success(true))
                },
                onReject = {
                    _pendingConfirmation.value = null
                    continuation.resumeWith(Result.success(false))
                }
            )
        }

    fun triggerListening() {
        if (_status.value == AgentStatus.LISTENING) {
            speechManager.stopListening()
            _status.value = AgentStatus.IDLE
        } else {
            speechManager.stopSpeaking()
            _status.value = AgentStatus.LISTENING
            speechManager.startListening(
                languageCode = _targetLanguage.value.code,
                onResult = { recognizedText ->
                    handleUserInput(recognizedText)
                },
                onError = { err ->
                    _status.value = AgentStatus.IDLE
                }
            )
        }
    }

    fun interruptSpeech() {
        speechManager.stopSpeaking()
        speechManager.stopListening()
        _status.value = AgentStatus.IDLE
    }
}
