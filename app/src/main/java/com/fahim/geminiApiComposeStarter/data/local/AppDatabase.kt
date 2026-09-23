package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        ConversationEntity::class,
        QuizAttemptEntity::class,
        UserMemoryEntity::class,
        FlashcardEntity::class,
        StudyPlanEntity::class,
        WeakTopicEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun weakTopicDao(): WeakTopicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alter chat_messages to add conversationId, isSaved, and imageUri
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN isSaved INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN imageUri TEXT DEFAULT NULL")

                // 2. Create conversations table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        mode TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // 3. Ensure a default conversation exists for previously persisted messages
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO conversations (id, title, createdAt, updatedAt, mode)
                    VALUES ('default', 'Main Study Session', $now, $now, 'CHAT')
                    """.trimIndent()
                )

                // 4. Create quiz_attempts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_attempts (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversationId TEXT NOT NULL,
                        topic TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        percentage INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        strongAreas TEXT NOT NULL,
                        revisionTopics TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create user_memories table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_memories (
                        id TEXT NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 2. Create flashcards table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS flashcards (
                        id TEXT NOT NULL PRIMARY KEY,
                        deckTitle TEXT NOT NULL,
                        front TEXT NOT NULL,
                        back TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        reviewCount INTEGER NOT NULL,
                        lastReviewedAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 3. Create study_plans table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_plans (
                        id TEXT NOT NULL PRIMARY KEY,
                        subject TEXT NOT NULL,
                        targetExamDate TEXT NOT NULL,
                        dailyTimeMinutes INTEGER NOT NULL,
                        planJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 4. Create weak_topics table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weak_topics (
                        id TEXT NOT NULL PRIMARY KEY,
                        topic TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        failureCount INTEGER NOT NULL,
                        successCount INTEGER NOT NULL,
                        lastPracticedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gemini_chat.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
