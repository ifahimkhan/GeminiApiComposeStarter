package com.fahim.geminiApiComposeStarter.ui.workflows

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.data.local.FlashcardEntity
import com.fahim.geminiApiComposeStarter.data.local.StudyPlanEntity
import com.fahim.geminiApiComposeStarter.model.InteractiveQuiz
import com.fahim.geminiApiComposeStarter.model.QuizQuestion
import com.fahim.geminiApiComposeStarter.model.StructuredOutputParser
import com.fahim.geminiApiComposeStarter.model.StructuredStudyPlan
import kotlinx.coroutines.delay

enum class WorkflowTab(val title: String) {
    QUIZ("Quiz"),
    FLASHCARDS("Flashcards"),
    EXAM("Exam Sim"),
    PLANNER("Study Plan"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyWorkflowsScreen(
    onGeneratePrompt: (String, (String) -> Unit) -> Unit,
    onSaveQuizScore: (topic: String, score: Int, total: Int, strong: String, revision: String) -> Unit,
    onSaveFlashcards: (List<FlashcardEntity>) -> Unit,
    onSaveStudyPlan: (StudyPlanEntity) -> Unit,
    onAddWeakTopic: (topic: String, subject: String) -> Unit,
    savedFlashcards: List<FlashcardEntity> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(WorkflowTab.QUIZ) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
        ) {
            WorkflowTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            WorkflowTab.QUIZ -> QuizWorkflowTab(
                onGeneratePrompt = onGeneratePrompt,
                onSaveQuizScore = onSaveQuizScore,
                onAddWeakTopic = onAddWeakTopic,
            )
            WorkflowTab.FLASHCARDS -> FlashcardsWorkflowTab(
                savedCards = savedFlashcards,
                onGeneratePrompt = onGeneratePrompt,
                onSaveFlashcards = onSaveFlashcards,
            )
            WorkflowTab.EXAM -> ExamSimulatorTab(
                onGeneratePrompt = onGeneratePrompt,
                onSaveQuizScore = onSaveQuizScore,
                onAddWeakTopic = onAddWeakTopic,
            )
            WorkflowTab.PLANNER -> StudyPlannerTab(
                onGeneratePrompt = onGeneratePrompt,
                onSaveStudyPlan = onSaveStudyPlan,
            )
        }
    }
}

// ── 1. Interactive Quiz Tab ──────────────────────────────────────────────────

@Composable
private fun QuizWorkflowTab(
    onGeneratePrompt: (String, (String) -> Unit) -> Unit,
    onSaveQuizScore: (String, Int, Int, String, String) -> Unit,
    onAddWeakTopic: (String, String) -> Unit,
) {
    var topicInput by remember { mutableStateOf("Computer Networks") }
    var isLoading by remember { mutableStateOf(false) }
    var activeQuiz by remember { mutableStateOf<InteractiveQuiz?>(null) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }
    val wrongQuestions = remember { mutableStateListOf<QuizQuestion>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (activeQuiz == null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Interactive Quiz Generator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gemini will craft custom multiple-choice questions with deep diagnostic explanations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Topic or Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (topicInput.isNotBlank()) {
                                    isLoading = true
                                    val prompt = """
                                        Generate an interactive study quiz on the topic '$topicInput'.
                                        Return strictly valid JSON with this schema:
                                        {
                                          "topic": "$topicInput",
                                          "questions": [
                                            {
                                              "question": "Question text",
                                              "options": ["Option A", "Option B", "Option C", "Option D"],
                                              "correctIndex": 0,
                                              "explanation": "Why this answer is correct",
                                              "topic": "Subtopic"
                                            }
                                          ]
                                        }
                                        Provide 4-5 high quality conceptual questions.
                                    """.trimIndent()

                                    onGeneratePrompt(prompt) { response ->
                                        isLoading = false
                                        val parsed = StructuredOutputParser.parseQuiz(response, topicInput)
                                        if (parsed != null && parsed.questions.isNotEmpty()) {
                                            activeQuiz = parsed
                                            currentQuestionIndex = 0
                                            selectedOptionIndex = null
                                            score = 0
                                            quizCompleted = false
                                            wrongQuestions.clear()
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading && topicInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Quiz...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Interactive Quiz")
                            }
                        }
                    }
                }
            }
        } else if (!quizCompleted) {
            val questions = activeQuiz!!.questions
            val currentQ = questions.getOrNull(currentQuestionIndex)

            if (currentQ != null) {
                item {
                    // Quiz Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Score: $score",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = currentQ.topic,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentQ.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Options list
                items(currentQ.options.indices.toList()) { index ->
                    val optionText = currentQ.options[index]
                    val isSelected = selectedOptionIndex == index
                    val isAnswerRevealed = selectedOptionIndex != null
                    val isCorrect = index == currentQ.correctIndex

                    val cardColor = when {
                        !isAnswerRevealed -> MaterialTheme.colorScheme.surface
                        isCorrect -> Color(0xFFE8F5E9)
                        isSelected && !isCorrect -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        !isAnswerRevealed && isSelected -> MaterialTheme.colorScheme.primary
                        isAnswerRevealed && isCorrect -> Color(0xFF4CAF50)
                        isAnswerRevealed && isSelected && !isCorrect -> Color(0xFFE53935)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = selectedOptionIndex == null) {
                                selectedOptionIndex = index
                                if (isCorrect) {
                                    score++
                                } else {
                                    wrongQuestions.add(currentQ)
                                    onAddWeakTopic(currentQ.topic, activeQuiz!!.topic)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${('A' + index)}. ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (isAnswerRevealed) {
                                if (isCorrect) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                } else if (isSelected) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFE53935))
                                }
                            }
                        }
                    }
                }

                if (selectedOptionIndex != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Explanation:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentQ.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (currentQuestionIndex < questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedOptionIndex = null
                                } else {
                                    quizCompleted = true
                                    val strongArea = if (score > 0) activeQuiz!!.topic else "None"
                                    val revision = wrongQuestions.joinToString(", ") { it.topic }.ifBlank { "None" }
                                    onSaveQuizScore(activeQuiz!!.topic, score, questions.size, strongArea, revision)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(if (currentQuestionIndex < questions.size - 1) "Next Question" else "View Results")
                        }
                    }
                }
            }
        } else {
            // Quiz Results Card
            val questions = activeQuiz!!.questions
            val pct = (score * 100) / questions.size

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Quiz Completed!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$score / ${questions.size}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 70) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "$pct% Mastery",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (wrongQuestions.isNotEmpty()) {
                            Text(
                                text = "Flagged Topics for Revision:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            wrongQuestions.map { it.topic }.distinct().forEach { wt ->
                                Text(
                                    text = "• $wt",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                            onClick = {
                                activeQuiz = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Take Another Quiz")
                        }
                    }
                }
            }
        }
    }
}

// ── 2. Flashcards Workflow Tab ───────────────────────────────────────────────

@Composable
private fun FlashcardsWorkflowTab(
    savedCards: List<FlashcardEntity>,
    onGeneratePrompt: (String, (String) -> Unit) -> Unit,
    onSaveFlashcards: (List<FlashcardEntity>) -> Unit,
) {
    var deckTopic by remember { mutableStateOf("Operating Systems") }
    var isLoading by remember { mutableStateOf(false) }
    var currentCards by remember { mutableStateOf(savedCards) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(savedCards) {
        if (currentCards.isEmpty() && savedCards.isNotEmpty()) {
            currentCards = savedCards
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Generator card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Flashcard Deck Creator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deckTopic,
                        onValueChange = { deckTopic = it },
                        label = { Text("Topic / Exam Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (deckTopic.isNotBlank()) {
                                isLoading = true
                                val prompt = """
                                    Create a high-yield study flashcard deck for the topic '$deckTopic'.
                                    Return strictly valid JSON with this schema:
                                    {
                                      "deckTitle": "$deckTopic",
                                      "cards": [
                                        {
                                          "front": "Question / Concept",
                                          "back": "Concise, precise answer / formula",
                                          "topic": "$deckTopic"
                                        }
                                      ]
                                    }
                                    Provide 5 conceptual flashcards.
                                """.trimIndent()

                                onGeneratePrompt(prompt) { response ->
                                    isLoading = false
                                    val parsed = StructuredOutputParser.parseFlashcards(response, deckTopic)
                                    if (parsed != null && parsed.cards.isNotEmpty()) {
                                        val entities = parsed.cards.map { card ->
                                            FlashcardEntity(
                                                deckTitle = parsed.deckTitle,
                                                front = card.front,
                                                back = card.back,
                                            )
                                        }
                                        currentCards = entities
                                        currentCardIndex = 0
                                        isFlipped = false
                                        onSaveFlashcards(entities)
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && deckTopic.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Deck...")
                        } else {
                            Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Flashcard Deck")
                        }
                    }
                }
            }
        }

        // Active Card Display
        if (currentCards.isNotEmpty()) {
            val activeCard = currentCards.getOrNull(currentCardIndex)
            if (activeCard != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Card ${currentCardIndex + 1} of ${currentCards.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Deck: ${activeCard.deckTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    // 3D Flip Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            }
                            .clickable { isFlipped = !isFlipped },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (rotation <= 90f) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = "FRONT / QUESTION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = activeCard.front,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Tap card to flip",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = "BACK / ANSWER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = activeCard.back,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                // Spaced Repetition Rating Buttons
                item {
                    Text(
                        text = "Rate Recall Quality (Spaced Repetition):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentCardIndex < currentCards.size - 1) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Again")
                        }
                        OutlinedButton(
                            onClick = {
                                if (currentCardIndex < currentCards.size - 1) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Hard")
                        }
                        FilledTonalButton(
                            onClick = {
                                if (currentCardIndex < currentCards.size - 1) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Good")
                        }
                        Button(
                            onClick = {
                                if (currentCardIndex < currentCards.size - 1) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Easy")
                        }
                    }
                }

                // Card Navigation Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = {
                                if (currentCardIndex > 0) {
                                    currentCardIndex--
                                    isFlipped = false
                                }
                            },
                            enabled = currentCardIndex > 0,
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                        }
                        IconButton(
                            onClick = { isFlipped = !isFlipped },
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = "Flip")
                        }
                        IconButton(
                            onClick = {
                                if (currentCardIndex < currentCards.size - 1) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            },
                            enabled = currentCardIndex < currentCards.size - 1,
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            }
        }
    }
}

// ── 3. Exam Simulator Tab ────────────────────────────────────────────────────

@Composable
private fun ExamSimulatorTab(
    onGeneratePrompt: (String, (String) -> Unit) -> Unit,
    onSaveQuizScore: (String, Int, Int, String, String) -> Unit,
    onAddWeakTopic: (String, String) -> Unit,
) {
    var examTopic by remember { mutableStateOf("Database Systems & SQL") }
    var examMinutes by remember { mutableIntStateOf(5) }
    var isExamRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var examQuiz by remember { mutableStateOf<InteractiveQuiz?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(300) }
    var userAnswers by remember { mutableStateOf(mapOf<Int, Int>()) }
    var examSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(isExamRunning, secondsRemaining) {
        if (isExamRunning && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        } else if (isExamRunning && secondsRemaining == 0) {
            examSubmitted = true
            isExamRunning = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!isExamRunning && !examSubmitted) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Timed Exam Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulates real exam conditions with strict timer, conceptual questions, and instant grading breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = examTopic,
                            onValueChange = { examTopic = it },
                            label = { Text("Exam Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(3, 5, 10, 15).forEach { min ->
                                OutlinedButton(
                                    onClick = { examMinutes = min },
                                    modifier = Modifier.weight(1f),
                                    colors = if (examMinutes == min) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("${min}m")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (examTopic.isNotBlank()) {
                                    isLoading = true
                                    val prompt = """
                                        Generate an exam test on '$examTopic'.
                                        Return strictly valid JSON:
                                        {
                                          "topic": "$examTopic",
                                          "questions": [
                                            {
                                              "question": "Question text",
                                              "options": ["A", "B", "C", "D"],
                                              "correctIndex": 0,
                                              "explanation": "Rationale",
                                              "topic": "Subtopic"
                                            }
                                          ]
                                        }
                                        Provide 5 university-level exam questions.
                                    """.trimIndent()

                                    onGeneratePrompt(prompt) { response ->
                                        isLoading = false
                                        val parsed = StructuredOutputParser.parseQuiz(response, examTopic)
                                        if (parsed != null && parsed.questions.isNotEmpty()) {
                                            examQuiz = parsed
                                            secondsRemaining = examMinutes * 60
                                            userAnswers = emptyMap()
                                            examSubmitted = false
                                            isExamRunning = true
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading && examTopic.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preparing Exam Paper...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Timed Exam")
                            }
                        }
                    }
                }
            }
        } else if (isExamRunning && examQuiz != null) {
            val minutes = secondsRemaining / 60
            val seconds = secondsRemaining % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (secondsRemaining < 60) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Time Left: $timeString",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = {
                                isExamRunning = false
                                examSubmitted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Submit Exam")
                        }
                    }
                }
            }

            // Questions list
            items(examQuiz!!.questions.indices.toList()) { qIdx ->
                val q = examQuiz!!.questions[qIdx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Question ${qIdx + 1}: ${q.question}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        q.options.forEachIndexed { optIdx, opt ->
                            val isChosen = userAnswers[qIdx] == optIdx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        userAnswers = userAnswers + (qIdx to optIdx)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${('A' + optIdx)}. $opt",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (examSubmitted && examQuiz != null) {
            val questions = examQuiz!!.questions
            var correctCount = 0
            val weak = mutableListOf<String>()

            questions.forEachIndexed { idx, q ->
                if (userAnswers[idx] == q.correctIndex) {
                    correctCount++
                } else {
                    weak.add(q.topic)
                    onAddWeakTopic(q.topic, examQuiz!!.topic)
                }
            }
            val percentage = (correctCount * 100) / questions.size

            item {
                LaunchedEffect(Unit) {
                    val strong = if (correctCount > 0) examQuiz!!.topic else "None"
                    val rev = weak.distinct().joinToString(", ").ifBlank { "None" }
                    onSaveQuizScore(examQuiz!!.topic, correctCount, questions.size, strong, rev)
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Exam Results",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$correctCount / ${questions.size}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (percentage >= 70) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "$percentage% Final Score",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                examQuiz = null
                                examSubmitted = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Take Another Exam")
                        }
                    }
                }
            }
        }
    }
}

// ── 4. Study Planner Tab ─────────────────────────────────────────────────────

@Composable
private fun StudyPlannerTab(
    onGeneratePrompt: (String, (String) -> Unit) -> Unit,
    onSaveStudyPlan: (StudyPlanEntity) -> Unit,
) {
    var subject by remember { mutableStateOf("Android App Development") }
    var examDate by remember { mutableStateOf("In 14 days") }
    var dailyMinutes by remember { mutableIntStateOf(60) }
    var isLoading by remember { mutableStateOf(false) }
    var generatedPlan by remember { mutableStateOf<StructuredStudyPlan?>(null) }
    var planSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Timetable & Study Plan Generator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject / Syllabus") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        label = { Text("Target Exam / Deadline") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (subject.isNotBlank()) {
                                isLoading = true
                                val prompt = """
                                    Create a structured, day-by-day exam study timetable for subject '$subject' targeting deadline '$examDate' with $dailyMinutes minutes daily.
                                    Return strictly valid JSON:
                                    {
                                      "subject": "$subject",
                                      "targetExamDate": "$examDate",
                                      "dailyTimeMinutes": $dailyMinutes,
                                      "days": [
                                        {
                                          "dayNumber": 1,
                                          "date": "Day 1",
                                          "topic": "Core Topic",
                                          "durationMinutes": $dailyMinutes,
                                          "objectives": ["Objective 1", "Objective 2"]
                                        }
                                      ]
                                    }
                                    Provide 5 structured study days.
                                """.trimIndent()

                                onGeneratePrompt(prompt) { response ->
                                    isLoading = false
                                    val parsed = StructuredOutputParser.parseStudyPlan(response, subject)
                                    if (parsed != null && parsed.days.isNotEmpty()) {
                                        generatedPlan = parsed
                                        planSaved = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && subject.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Building Timetable...")
                        } else {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Study Plan")
                        }
                    }
                }
            }
        }

        if (generatedPlan != null) {
            val plan = generatedPlan!!

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = plan.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Target: ${plan.targetExamDate} · ${plan.dailyTimeMinutes} min/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Button(
                        onClick = {
                            val entity = StudyPlanEntity(
                                subject = plan.subject,
                                targetExamDate = plan.targetExamDate,
                                dailyTimeMinutes = plan.dailyTimeMinutes,
                                planJson = plan.days.joinToString("\n") { "Day ${it.dayNumber}: ${it.topic}" },
                            )
                            onSaveStudyPlan(entity)
                            planSaved = true
                        },
                        enabled = !planSaved,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(if (planSaved) Icons.Default.Check else Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (planSaved) "Saved" else "Save Plan")
                    }
                }
            }

            items(plan.days) { day ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Day ${day.dayNumber} · ${day.topic}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${day.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        day.objectives.forEach { obj ->
                            Text(
                                text = "• $obj",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
