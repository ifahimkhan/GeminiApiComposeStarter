package com.fahim.geminiApiComposeStarter.model

import org.json.JSONArray
import org.json.JSONObject

// ── Interactive Quiz Models ──────────────────────────────────────────────────

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val topic: String,
)

data class InteractiveQuiz(
    val topic: String,
    val questions: List<QuizQuestion>,
)

// ── Flashcard Models ─────────────────────────────────────────────────────────

data class FlashcardItem(
    val front: String,
    val back: String,
    val topic: String = "General",
)

data class FlashcardDeck(
    val deckTitle: String,
    val cards: List<FlashcardItem>,
)

// ── Study Plan Models ────────────────────────────────────────────────────────

data class StudyDay(
    val dayNumber: Int,
    val date: String,
    val topic: String,
    val durationMinutes: Int,
    val objectives: List<String>,
)

data class StructuredStudyPlan(
    val subject: String,
    val targetExamDate: String,
    val dailyTimeMinutes: Int,
    val days: List<StudyDay>,
)

// ── Robust Parsers & Schemas ─────────────────────────────────────────────────

object StructuredOutputParser {

    private fun extractJsonBlock(rawText: String): String {
        val trimmed = rawText.trim()
        val fencedMatch = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE).find(trimmed)
        if (fencedMatch != null) {
            return fencedMatch.groupValues[1].trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim()
        }
        val firstBracket = trimmed.indexOf('[')
        val lastBracket = trimmed.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return trimmed.substring(firstBracket, lastBracket + 1).trim()
        }
        return trimmed
    }

    fun parseQuiz(rawText: String, defaultTopic: String = "Practice Quiz"): InteractiveQuiz? {
        return try {
            val jsonStr = extractJsonBlock(rawText)
            val json = JSONObject(jsonStr)
            val topic = json.optString("topic", defaultTopic)
            val questionsArray = json.getJSONArray("questions")
            val questions = mutableListOf<QuizQuestion>()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val qText = qObj.getString("question")
                val optArray = qObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optArray.length()) {
                    options.add(optArray.getString(j))
                }
                val correctIndex = qObj.optInt("correctIndex", 0)
                val explanation = qObj.optString("explanation", "")
                val qTopic = qObj.optString("topic", topic)

                questions.add(
                    QuizQuestion(
                        question = qText,
                        options = options,
                        correctIndex = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
                        explanation = explanation,
                        topic = qTopic,
                    )
                )
            }

            if (questions.isNotEmpty()) InteractiveQuiz(topic, questions) else null
        } catch (e: Exception) {
            null
        }
    }

    fun parseFlashcards(rawText: String, defaultTitle: String = "Study Deck"): FlashcardDeck? {
        return try {
            val jsonStr = extractJsonBlock(rawText)
            val cards = mutableListOf<FlashcardItem>()
            var title = defaultTitle

            if (jsonStr.startsWith("{")) {
                val json = JSONObject(jsonStr)
                title = json.optString("deckTitle", defaultTitle)
                val cardsArray = json.optJSONArray("cards") ?: json.getJSONArray("flashcards")
                for (i in 0 until cardsArray.length()) {
                    val cardObj = cardsArray.getJSONObject(i)
                    cards.add(
                        FlashcardItem(
                            front = cardObj.getString("front"),
                            back = cardObj.getString("back"),
                            topic = cardObj.optString("topic", title),
                        )
                    )
                }
            } else if (jsonStr.startsWith("[")) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val cardObj = array.getJSONObject(i)
                    cards.add(
                        FlashcardItem(
                            front = cardObj.getString("front"),
                            back = cardObj.getString("back"),
                            topic = cardObj.optString("topic", title),
                        )
                    )
                }
            }

            if (cards.isNotEmpty()) FlashcardDeck(title, cards) else null
        } catch (e: Exception) {
            null
        }
    }

    fun parseStudyPlan(rawText: String, defaultSubject: String = "Study Plan"): StructuredStudyPlan? {
        return try {
            val jsonStr = extractJsonBlock(rawText)
            val json = JSONObject(jsonStr)
            val subject = json.optString("subject", defaultSubject)
            val examDate = json.optString("targetExamDate", "Upcoming Exam")
            val dailyMinutes = json.optInt("dailyTimeMinutes", 60)
            val daysArray = json.getJSONArray("days")
            val days = mutableListOf<StudyDay>()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val dayNum = dayObj.optInt("dayNumber", i + 1)
                val date = dayObj.optString("date", "Day $dayNum")
                val topic = dayObj.getString("topic")
                val duration = dayObj.optInt("durationMinutes", dailyMinutes)

                val objArray = dayObj.optJSONArray("objectives")
                val objectives = mutableListOf<String>()
                if (objArray != null) {
                    for (j in 0 until objArray.length()) {
                        objectives.add(objArray.getString(j))
                    }
                }

                days.add(
                    StudyDay(
                        dayNumber = dayNum,
                        date = date,
                        topic = topic,
                        durationMinutes = duration,
                        objectives = objectives,
                    )
                )
            }

            if (days.isNotEmpty()) {
                StructuredStudyPlan(
                    subject = subject,
                    targetExamDate = examDate,
                    dailyTimeMinutes = dailyMinutes,
                    days = days,
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
