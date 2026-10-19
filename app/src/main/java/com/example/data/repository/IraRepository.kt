package com.example.data.repository

import com.example.data.db.MemoryDao
import com.example.data.db.ProductivityDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.EpisodicMemoryEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ProceduralRoutineEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.SemanticFactEntity
import kotlinx.coroutines.flow.Flow

class IraRepository(
    private val memoryDao: MemoryDao,
    private val productivityDao: ProductivityDao
) {
    // --- Episodic Memories ---
    val recentMemories: Flow<List<EpisodicMemoryEntity>> = memoryDao.getRecentMemories()

    suspend fun recordEpisode(
        userPrompt: String,
        iraResponse: String,
        subAgent: String,
        language: String,
        sentiment: String,
        planSummary: String
    ): Long {
        return memoryDao.insertMemory(
            EpisodicMemoryEntity(
                userPrompt = userPrompt,
                iraResponse = iraResponse,
                subAgent = subAgent,
                language = language,
                sentiment = sentiment,
                planSummary = planSummary
            )
        )
    }

    suspend fun clearEpisodicMemories() = memoryDao.clearEpisodicMemories()

    // --- Semantic Facts (RAG) ---
    val allFacts: Flow<List<SemanticFactEntity>> = memoryDao.getAllFacts()

    suspend fun getFact(key: String): SemanticFactEntity? = memoryDao.getFactByKey(key)

    suspend fun saveFact(category: String, key: String, value: String) {
        memoryDao.insertFact(
            SemanticFactEntity(
                category = category,
                factKey = key,
                factValue = value
            )
        )
    }

    suspend fun searchSemanticMemory(query: String): List<SemanticFactEntity> {
        return memoryDao.searchFacts(query)
    }

    suspend fun deleteFact(id: Long) = memoryDao.deleteFact(id)

    // --- Procedural Routines ---
    val allRoutines: Flow<List<ProceduralRoutineEntity>> = memoryDao.getAllRoutines()

    suspend fun saveRoutine(routine: ProceduralRoutineEntity) = memoryDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: ProceduralRoutineEntity) = memoryDao.updateRoutine(routine)

    suspend fun deleteRoutine(id: Long) = memoryDao.deleteRoutine(id)

    // --- Audit Logs ---
    val auditLogs: Flow<List<AuditLogEntity>> = memoryDao.getRecentAuditLogs()

    suspend fun logAudit(
        toolName: String,
        params: String,
        risk: String,
        status: String,
        details: String
    ): Long {
        return memoryDao.insertAuditLog(
            AuditLogEntity(
                toolName = toolName,
                parametersJson = params,
                riskLevel = risk,
                status = status,
                details = details
            )
        )
    }

    suspend fun clearAuditLogs() = memoryDao.clearAuditLogs()

    // --- Productivity: Notes & Reminders ---
    val notes: Flow<List<NoteEntity>> = productivityDao.getAllNotes()

    suspend fun createNote(title: String, content: String, category: String = "General"): Long {
        return productivityDao.insertNote(
            NoteEntity(title = title, content = content, category = category)
        )
    }

    suspend fun searchNotes(query: String): List<NoteEntity> = productivityDao.searchNotes(query)

    suspend fun deleteNote(id: Long) = productivityDao.deleteNote(id)

    val reminders: Flow<List<ReminderEntity>> = productivityDao.getAllReminders()

    suspend fun addReminder(title: String, targetTime: String, priority: String = "Normal"): Long {
        return productivityDao.insertReminder(
            ReminderEntity(title = title, targetTime = targetTime, priority = priority)
        )
    }

    suspend fun completeReminder(id: Long, completed: Boolean) =
        productivityDao.setReminderCompleted(id, completed)

    suspend fun deleteReminder(id: Long) = productivityDao.deleteReminder(id)
}
