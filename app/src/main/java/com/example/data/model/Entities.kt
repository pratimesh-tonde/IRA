package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodic_memories")
data class EpisodicMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userPrompt: String,
    val iraResponse: String,
    val subAgent: String,
    val language: String,
    val sentiment: String,
    val planSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "semantic_facts")
data class SemanticFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // e.g. "Personal", "Preference", "Contact", "Routine"
    val factKey: String,   // e.g. "user_name", "home_city", "favorite_music"
    val factValue: String, // e.g. "Pratimesh", "Pune", "Lo-Fi"
    val confidence: Float = 1.0f,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "procedural_routines")
data class ProceduralRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerName: String,     // e.g. "Night Mode Protocol", "Low Battery"
    val conditionDesc: String,   // e.g. "Triggered when battery < 20% or after 10 PM"
    val stepsJson: String,       // JSON or comma list of tools/actions
    val executionCount: Int = 0,
    val isEnabled: Boolean = true,
    val lastExecuted: Long = 0
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val parametersJson: String,
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
    val status: String,    // "SUCCESS", "FAILED", "CONFIRMED", "BLOCKED"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetTime: String,
    val isCompleted: Boolean = false,
    val priority: String = "Normal",
    val timestamp: Long = System.currentTimeMillis()
)
