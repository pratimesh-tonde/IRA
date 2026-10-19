package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.EpisodicMemoryEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ProceduralRoutineEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.SemanticFactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EpisodicMemoryEntity::class,
        SemanticFactEntity::class,
        ProceduralRoutineEntity::class,
        AuditLogEntity::class,
        NoteEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IraDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun productivityDao(): ProductivityDao

    companion object {
        @Volatile
        private var INSTANCE: IraDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): IraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IraDatabase::class.java,
                    "ira_agentic_offline.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.memoryDao(), database.productivityDao())
                    }
                }
            }

            private suspend fun populateInitialData(memoryDao: MemoryDao, productivityDao: ProductivityDao) {
                // Pre-populate semantic memory facts
                memoryDao.insertFact(
                    SemanticFactEntity(
                        category = "System",
                        factKey = "assistant_identity",
                        factValue = "IRA - Offline Agentic AI by AIX Tech Private Limited"
                    )
                )
                memoryDao.insertFact(
                    SemanticFactEntity(
                        category = "User",
                        factKey = "user_name",
                        factValue = "Pratimesh"
                    )
                )
                memoryDao.insertFact(
                    SemanticFactEntity(
                        category = "Preference",
                        factKey = "preferred_languages",
                        factValue = "Hindi, Marathi, English"
                    )
                )
                memoryDao.insertFact(
                    SemanticFactEntity(
                        category = "Preference",
                        factKey = "security_mode",
                        factValue = "Strict Sandboxed Confirmation Gate"
                    )
                )

                // Pre-populate procedural routines
                memoryDao.insertRoutine(
                    ProceduralRoutineEntity(
                        triggerName = "Low Battery Protocol",
                        conditionDesc = "Device battery falls below 20%",
                        stepsJson = "check_battery -> notify_user -> dim_screen_hint -> save_session_state",
                        executionCount = 1,
                        isEnabled = true
                    )
                )
                memoryDao.insertRoutine(
                    ProceduralRoutineEntity(
                        triggerName = "Night Rest Protocol",
                        conditionDesc = "Scheduled at 10:30 PM daily",
                        stepsJson = "check_alarms -> silence_ringtone_intent -> summarize_day_notes",
                        executionCount = 0,
                        isEnabled = true
                    )
                )
                memoryDao.insertRoutine(
                    ProceduralRoutineEntity(
                        triggerName = "Morning Briefing",
                        conditionDesc = "First activation between 6:00 AM - 9:00 AM",
                        stepsJson = "get_current_time -> list_reminders -> check_battery_status",
                        executionCount = 2,
                        isEnabled = true
                    )
                )

                // Pre-populate sample notes
                productivityDao.insertNote(
                    NoteEntity(
                        title = "IRA Offline Agent Initialized",
                        content = "IRA v2 is completely self-contained. All planning, tools, and 3-tier memory execute on-device.",
                        category = "System"
                    )
                )

                // Pre-populate sample reminder
                productivityDao.insertReminder(
                    ReminderEntity(
                        title = "Check evening task progress",
                        targetTime = "07:00 PM",
                        priority = "High"
                    )
                )
            }
        }
    }
}
