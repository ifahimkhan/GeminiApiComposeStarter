package com.fahim.geminiApiComposeStarter.model

/**
 * Educational AI study modes supported by the Gemini AI Study Workspace.
 * Each mode activates a dedicated pedagogical workflow and instruction strategy.
 */
enum class StudyMode(
    val title: String,
    val description: String,
    val badgeLabel: String,
    val quickPrompts: List<String>,
) {
    CHAT(
        title = "General Chat",
        description = "Open-ended conversational assistant for general academic inquiries.",
        badgeLabel = "Chat",
        quickPrompts = listOf(
            "Help me plan my study schedule for exams",
            "What are effective techniques for active recall?",
            "How do I balance coursework and projects?",
        ),
    ),
    EXPLAIN(
        title = "Concept Explainer",
        description = "Structured breakdown: core intuition, real-world examples, pitfalls, and check questions.",
        badgeLabel = "Explain",
        quickPrompts = listOf(
            "Explain Fourier Transform from intuition to math",
            "Explain Database Normalization (1NF to BCNF)",
            "Explain how Kotlin Coroutines work under the hood",
        ),
    ),
    STUDY(
        title = "Socratic Study Tutor",
        description = "Interactive guided learning: teaches one concept at a time and quizzes you along the way.",
        badgeLabel = "Study",
        quickPrompts = listOf(
            "Teach me Binary Search Trees step-by-step",
            "Guide me through Process Scheduling in Operating Systems",
            "Help me understand Dijkstra's Algorithm interactively",
        ),
    ),
    QUIZ(
        title = "Practice Quiz",
        description = "Generates targeted multiple-choice practice questions and evaluates your answers.",
        badgeLabel = "Quiz",
        quickPrompts = listOf(
            "Quiz me on Computer Networks OSI Model (5 questions)",
            "Quiz me on Android Jetpack Compose State Management",
            "Quiz me on SQL Joins and Aggregations",
        ),
    ),
    CHALLENGE(
        title = "Deep Challenge",
        description = "Critical thinking: spot subtle bugs, find logical flaws, and defend technical decisions.",
        badgeLabel = "Challenge",
        quickPrompts = listOf(
            "Challenge me to spot the concurrency bug in this pseudo-code",
            "Give me an architectural trade-off scenario to defend",
            "Test my understanding of edge cases in QuickSort",
        ),
    ),
    SUMMARIZE(
        title = "Lecture Summarizer",
        description = "Condenses notes into TL;DR, key formulas, terminology glossary, and exam points.",
        badgeLabel = "Summarize",
        quickPrompts = listOf(
            "Summarize the key differences between TCP and UDP",
            "Create exam revision notes on ACID properties in DBMS",
            "Extract key terms and definitions from my notes",
        ),
    ),
    CODE_REVIEW(
        title = "Code Reviewer",
        description = "Static analysis: inspects bugs, security pitfalls, time complexity, and offers cleaner code.",
        badgeLabel = "Code Review",
        quickPrompts = listOf(
            "Review this Kotlin function for edge-case errors",
            "Analyze the time and space complexity of this snippet",
            "Suggest performance improvements for this Compose UI code",
        ),
    ),
    IMAGE_ANALYSIS(
        title = "Diagram & Math Solver",
        description = "Multimodal analysis: breaks down textbook diagrams, formulas, and whiteboard sketches.",
        badgeLabel = "Image Analysis",
        quickPrompts = listOf(
            "Explain the architecture shown in this diagram",
            "Solve the math problem in this image step-by-step",
            "Transcribe and explain this whiteboard algorithm",
        ),
    ),
    FLASHCARDS(
        title = "Flashcard Generator",
        description = "Generates front/back concept cards for active recall and spaced repetition practice.",
        badgeLabel = "Flashcards",
        quickPrompts = listOf(
            "Generate 5 flashcards on Database Indexing and B+ Trees",
            "Make flashcards for HTTP Status Codes and Headers",
            "Create flashcards for CPU Scheduling Algorithms",
        ),
    ),
    EXAM_SIMULATOR(
        title = "Timed Exam Simulator",
        description = "Full examination mode with countdown timer, strict scoring, and comprehensive review.",
        badgeLabel = "Exam",
        quickPrompts = listOf(
            "Simulate a 10-minute exam on Object-Oriented Design Patterns",
            "Start an exam simulation on Operating System Deadlocks",
            "Give me a timed exam on Computer Architecture pipelines",
        ),
    ),
    STUDY_PLANNER(
        title = "Study Schedule Planner",
        description = "Produces a realistic day-by-day study schedule based on available time and exam dates.",
        badgeLabel = "Planner",
        quickPrompts = listOf(
            "Plan a 7-day study schedule for Data Structures (2 hours/day)",
            "Create an exam prep roadmap for Operating Systems in 5 days",
            "Build a study timetable for Mobile Application Development",
        ),
    ),
    REVISION(
        title = "Targeted Revision Mode",
        description = "Retrieves previously missed quiz topics and drills them until mastered.",
        badgeLabel = "Revision",
        quickPrompts = listOf(
            "Review my weak topics and give me drill questions",
            "Help me revise concepts I struggled with in recent quizzes",
            "Generate targeted practice on my lowest-scoring areas",
        ),
    ),
    WEAK_TOPIC_DETECTOR(
        title = "Diagnostic Assessment",
        description = "Probes your understanding across the curriculum to diagnose knowledge gaps.",
        badgeLabel = "Diagnostic",
        quickPrompts = listOf(
            "Diagnose my knowledge gaps in Computer Networks",
            "Run a diagnostic test on Database Normalization",
            "Find my weak spots in Algorithms and Asymptotics",
        ),
    ),
    ASSIGNMENT_ASSISTANT(
        title = "Assignment & Rubric Guide",
        description = "Provides architectural guidance, edge-case checking, and rubric compliance.",
        badgeLabel = "Assignment",
        quickPrompts = listOf(
            "Check my design against standard assignment rubric criteria",
            "Help me structure my technical report for lab submission",
            "Verify edge cases for my database assignment schema",
        ),
    );

    companion object {
        val default = CHAT
    }
}
