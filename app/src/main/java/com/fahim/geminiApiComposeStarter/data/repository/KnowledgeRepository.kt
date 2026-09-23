package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.FlashcardDao
import com.fahim.geminiApiComposeStarter.data.local.FlashcardEntity
import com.fahim.geminiApiComposeStarter.data.local.FlashcardRating
import com.fahim.geminiApiComposeStarter.data.local.StudyPlanDao
import com.fahim.geminiApiComposeStarter.data.local.StudyPlanEntity
import com.fahim.geminiApiComposeStarter.data.local.WeakTopicDao
import com.fahim.geminiApiComposeStarter.data.local.WeakTopicEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class KnowledgeSearchResult(
    val type: KnowledgeItemType,
    val title: String,
    val snippet: String,
    val id: String,
)

enum class KnowledgeItemType {
    SAVED_NOTE,
    FLASHCARD,
    STUDY_PLAN,
    WEAK_TOPIC,
}

class KnowledgeRepository(
    private val flashcardDao: FlashcardDao,
    private val studyPlanDao: StudyPlanDao,
    private val weakTopicDao: WeakTopicDao,
    private val chatMessageDao: ChatMessageDao,
) {
    // ── Flashcards ───────────────────────────────────────────────────────────
    val allFlashcards: Flow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards()
    val deckTitles: Flow<List<String>> = flashcardDao.getAllDeckTitles()

    fun getFlashcardsForDeck(deckTitle: String): Flow<List<FlashcardEntity>> =
        flashcardDao.getFlashcardsForDeck(deckTitle)

    suspend fun insertFlashcards(deckTitle: String, cards: List<Pair<String, String>>) {
        val entities = cards.map { (front, back) ->
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                deckTitle = deckTitle,
                front = front,
                back = back,
            )
        }
        flashcardDao.insertFlashcards(entities)
    }

    suspend fun recordFlashcardReview(id: String, rating: FlashcardRating) {
        flashcardDao.recordReview(id, rating.name, System.currentTimeMillis())
    }

    suspend fun saveFlashcards(cards: List<FlashcardEntity>) {
        flashcardDao.insertFlashcards(cards)
    }

    suspend fun deleteFlashcard(id: String) {
        flashcardDao.deleteFlashcard(id)
    }

    suspend fun deleteDeck(deckTitle: String) {
        flashcardDao.deleteDeck(deckTitle)
    }

    // ── Study Plans ──────────────────────────────────────────────────────────
    val allPlans: Flow<List<StudyPlanEntity>> = studyPlanDao.getAllPlans()
    val activePlan: Flow<StudyPlanEntity?> = studyPlanDao.getActivePlan()

    suspend fun saveStudyPlan(
        subject: String,
        targetExamDate: String,
        dailyMinutes: Int,
        planJson: String,
    ): StudyPlanEntity {
        val plan = StudyPlanEntity(
            id = UUID.randomUUID().toString(),
            subject = subject,
            targetExamDate = targetExamDate,
            dailyTimeMinutes = dailyMinutes,
            planJson = planJson,
            isActive = true,
        )
        studyPlanDao.insertPlan(plan)
        studyPlanDao.setActivePlanOnly(plan.id)
        return plan
    }

    suspend fun saveStudyPlan(plan: StudyPlanEntity) {
        studyPlanDao.insertPlan(plan)
    }

    suspend fun deletePlan(id: String) {
        studyPlanDao.deletePlan(id)
    }

    suspend fun deleteStudyPlan(id: String) {
        studyPlanDao.deletePlan(id)
    }

    // ── Weak Topics ──────────────────────────────────────────────────────────
    val weakTopics: Flow<List<WeakTopicEntity>> = weakTopicDao.getAllWeakTopics()

    suspend fun recordWeakTopic(topicName: String, subjectName: String = "General") {
        val existing = weakTopicDao.findByTopic(topicName)
        if (existing != null) {
            val updated = existing.copy(
                failureCount = existing.failureCount + 1,
                lastPracticedAt = System.currentTimeMillis(),
            )
            weakTopicDao.updateWeakTopic(updated)
        } else {
            val newTopic = WeakTopicEntity(
                id = UUID.randomUUID().toString(),
                topic = topicName,
                subject = subjectName,
                failureCount = 1,
                successCount = 0,
                lastPracticedAt = System.currentTimeMillis(),
            )
            weakTopicDao.insertWeakTopic(newTopic)
        }
    }

    suspend fun recordWeakTopicMistake(topicName: String, subjectName: String = "General") {
        recordWeakTopic(topicName, subjectName)
    }

    suspend fun resolveWeakTopic(id: String) {
        weakTopicDao.deleteWeakTopic(id)
    }

    suspend fun recordTopicSuccess(topicName: String) {
        val existing = weakTopicDao.findByTopic(topicName)
        if (existing != null) {
            val updated = existing.copy(
                successCount = existing.successCount + 1,
                lastPracticedAt = System.currentTimeMillis(),
            )
            weakTopicDao.updateWeakTopic(updated)
        }
    }

    suspend fun clearAll() {
        flashcardDao.clearAll()
        studyPlanDao.clearAll()
        weakTopicDao.clearAll()
    }

    // ── Unified Search ───────────────────────────────────────────────────────
    fun searchKnowledge(query: String): Flow<List<KnowledgeSearchResult>> {
        val cleanQuery = query.trim().lowercase()
        return combine(
            chatMessageDao.getSavedMessages(),
            flashcardDao.getAllFlashcards(),
            studyPlanDao.getAllPlans(),
            weakTopicDao.getAllWeakTopics(),
        ) { savedNotes, cards, plans, weakTopics ->
            if (cleanQuery.isBlank()) {
                emptyList()
            } else {
                val results = mutableListOf<KnowledgeSearchResult>()

                // Saved notes matches
                savedNotes.filter { it.content.lowercase().contains(cleanQuery) }
                    .forEach { note ->
                        results.add(
                            KnowledgeSearchResult(
                                type = KnowledgeItemType.SAVED_NOTE,
                                title = "Saved Note",
                                snippet = note.content.take(120),
                                id = note.id,
                            )
                        )
                    }

                // Flashcard matches
                cards.filter {
                    it.deckTitle.lowercase().contains(cleanQuery) ||
                        it.front.lowercase().contains(cleanQuery) ||
                        it.back.lowercase().contains(cleanQuery)
                }.forEach { card ->
                    results.add(
                        KnowledgeSearchResult(
                            type = KnowledgeItemType.FLASHCARD,
                            title = "Flashcard: ${card.deckTitle}",
                            snippet = "Q: ${card.front}\nA: ${card.back}".take(120),
                            id = card.id,
                        )
                    )
                }

                // Study plans matches
                plans.filter { it.subject.lowercase().contains(cleanQuery) || it.planJson.lowercase().contains(cleanQuery) }
                    .forEach { plan ->
                        results.add(
                            KnowledgeSearchResult(
                                type = KnowledgeItemType.STUDY_PLAN,
                                title = "Study Plan: ${plan.subject}",
                                snippet = "Exam on: ${plan.targetExamDate} (${plan.dailyTimeMinutes} min/day)",
                                id = plan.id,
                            )
                        )
                    }

                // Weak topics matches
                weakTopics.filter { it.topic.lowercase().contains(cleanQuery) || it.subject.lowercase().contains(cleanQuery) }
                    .forEach { topic ->
                        results.add(
                            KnowledgeSearchResult(
                                type = KnowledgeItemType.WEAK_TOPIC,
                                title = "Weak Topic: ${topic.topic}",
                                snippet = "Failures: ${topic.failureCount}, Successes: ${topic.successCount}",
                                id = topic.id,
                            )
                        )
                    }

                results
            }
        }
    }
}
