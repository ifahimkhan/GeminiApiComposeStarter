package com.fahim.geminiApiComposeStarter.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StructuredModelsTest {

    @Test
    fun `parseQuiz parses valid JSON block successfully`() {
        val json = """
            ```json
            {
              "topic": "Operating Systems",
              "questions": [
                {
                  "question": "Which scheduling algorithm is non-preemptive?",
                  "options": ["Round Robin", "First Come First Served", "SRTF", "Priority (Preemptive)"],
                  "correctIndex": 1,
                  "explanation": "FCFS processes requests strictly in arrival order without preemption.",
                  "topic": "Process Scheduling"
                }
              ]
            }
            ```
        """.trimIndent()

        val quiz = StructuredOutputParser.parseQuiz(json)
        assertNotNull(quiz)
        assertEquals("Operating Systems", quiz?.topic)
        assertEquals(1, quiz?.questions?.size)
        assertEquals("Which scheduling algorithm is non-preemptive?", quiz?.questions?.first()?.question)
        assertEquals(1, quiz?.questions?.first()?.correctIndex)
    }

    @Test
    fun `parseFlashcards parses valid JSON deck successfully`() {
        val json = """
            {
              "deckTitle": "Computer Networks",
              "cards": [
                {
                  "front": "What layer does IP operate at?",
                  "back": "Network Layer (Layer 3)",
                  "topic": "OSI Model"
                },
                {
                  "front": "What does DNS stand for?",
                  "back": "Domain Name System",
                  "topic": "Application Layer"
                }
              ]
            }
        """.trimIndent()

        val deck = StructuredOutputParser.parseFlashcards(json)
        assertNotNull(deck)
        assertEquals("Computer Networks", deck?.deckTitle)
        assertEquals(2, deck?.cards?.size)
        assertEquals("What layer does IP operate at?", deck?.cards?.first()?.front)
    }

    @Test
    fun `parseStudyPlan parses timetable successfully`() {
        val json = """
            ```json
            {
              "subject": "Database Management Systems",
              "targetExamDate": "2026-10-15",
              "dailyTimeMinutes": 90,
              "days": [
                {
                  "dayNumber": 1,
                  "date": "Monday",
                  "topic": "ER Diagrams & Relational Model",
                  "durationMinutes": 90,
                  "objectives": ["Understand entities and relationships", "Convert ER to Tables"]
                }
              ]
            }
            ```
        """.trimIndent()

        val plan = StructuredOutputParser.parseStudyPlan(json)
        assertNotNull(plan)
        assertEquals("Database Management Systems", plan?.subject)
        assertEquals(1, plan?.days?.size)
        assertEquals("ER Diagrams & Relational Model", plan?.days?.first()?.topic)
        assertEquals(2, plan?.days?.first()?.objectives?.size)
    }
}
