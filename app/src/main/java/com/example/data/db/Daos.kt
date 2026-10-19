package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLogEntity
import com.example.data.model.EpisodicMemoryEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ProceduralRoutineEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.SemanticFactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    // --- Episodic Memories ---
    @Query("SELECT * FROM episodic_memories ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMemories(): Flow<List<EpisodicMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: EpisodicMemoryEntity): Long

    @Query("DELETE FROM episodic_memories")
    suspend fun clearEpisodicMemories()

    // --- Semantic Facts ---
    @Query("SELECT * FROM semantic_facts ORDER BY updatedAt DESC")
    fun getAllFacts(): Flow<List<SemanticFactEntity>>

    @Query("SELECT * FROM semantic_facts WHERE factKey = :key LIMIT 1")
    suspend fun getFactByKey(key: String): SemanticFactEntity?

    @Query("SELECT * FROM semantic_facts WHERE factKey LIKE '%' || :query || '%' OR factValue LIKE '%' || :query || '%'")
    suspend fun searchFacts(query: String): List<SemanticFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: SemanticFactEntity): Long

    @Query("DELETE FROM semantic_facts WHERE id = :id")
    suspend fun deleteFact(id: Long)

    // --- Procedural Routines ---
    @Query("SELECT * FROM procedural_routines ORDER BY executionCount DESC")
    fun getAllRoutines(): Flow<List<ProceduralRoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: ProceduralRoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: ProceduralRoutineEntity)

    @Query("DELETE FROM procedural_routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}

@Dao
interface ProductivityDao {
    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // --- Reminders ---
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setReminderCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}
