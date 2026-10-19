package com.example.agent

enum class AgentStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    CONFIRMING,
    EXECUTING,
    ERROR
}

enum class SubAgentType(val displayName: String, val description: String) {
    COMMUNICATION("Comm Specialist", "Handles calls, SMS, contacts, and messaging"),
    PRODUCTIVITY("Task Executive", "Manages notes, reminders, alarms, and timers"),
    DEVICE("System Controller", "Controls flashlight, volume, battery, and device settings"),
    KNOWLEDGE("Cognitive Specialist", "Performs calculations, conversions, offline RAG & translations")
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class StepExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class PlanStep(
    val stepIndex: Int,
    val title: String,
    val toolName: String,
    val arguments: Map<String, String>,
    val riskLevel: RiskLevel,
    var status: StepExecutionStatus = StepExecutionStatus.PENDING,
    var thought: String = "",
    var observation: String = ""
)

data class ExecutionPlan(
    val goal: String,
    val subAgent: SubAgentType,
    val steps: List<PlanStep>,
    var currentStepIndex: Int = 0,
    var isCompleted: Boolean = false,
    var reflectionLog: String = ""
)

data class ConfirmationRequest(
    val toolName: String,
    val title: String,
    val description: String,
    val arguments: Map<String, String>,
    val riskLevel: RiskLevel = RiskLevel.HIGH,
    val onConfirm: () -> Unit,
    val onReject: () -> Unit
)
