package com.fahim.geminiApiComposeStarter.core.memory

import com.fahim.geminiApiComposeStarter.data.local.MemoryCategory
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import com.fahim.geminiApiComposeStarter.data.repository.UserMemoryRepository

sealed class MemoryCommandResult {
    data class Saved(val fact: String, val category: MemoryCategory) : MemoryCommandResult()
    data class Forgotten(val keyword: String, val count: Int) : MemoryCommandResult()
    data class ListAll(val memories: List<UserMemoryEntity>) : MemoryCommandResult()
    data object ClearedAll : MemoryCommandResult()
    data object NotACommand : MemoryCommandResult()
}

class MemoryEngine(
    private val memoryRepository: UserMemoryRepository,
) {

    suspend fun getMemoriesSnapshot(): List<UserMemoryEntity> =
        memoryRepository.getMemoriesSnapshot()

    suspend fun processNaturalCommand(prompt: String): MemoryCommandResult {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase().trimEnd('.', '!', '?', ',')

        // 1. "Forget everything you remember about me" / "Clear all memories"
        if (lower == "forget everything you remember about me" ||
            lower == "forget everything" ||
            lower == "clear all memories" ||
            lower == "clear memories"
        ) {
            memoryRepository.clearAll()
            return MemoryCommandResult.ClearedAll
        }

        // 2. "What do you remember about me?" / "Show my memories"
        if (lower.contains("what do you remember about me") ||
            lower.contains("what do you know about me") ||
            lower == "show my memories" ||
            lower == "show memories" ||
            lower == "what memories do you have"
        ) {
            val memories = memoryRepository.getMemoriesSnapshot()
            return MemoryCommandResult.ListAll(memories)
        }

        // 3. "Forget my name." / "Forget my name" / "Forget name"
        if (lower == "forget my name" || lower == "forget name") {
            val count = memoryRepository.deleteName()
            return MemoryCommandResult.Forgotten("my name", count)
        }

        // 4. "Forget that..." / "Forget [keyword]"
        if (lower.startsWith("forget that ") || lower.startsWith("forget ")) {
            val rawKeyword = when {
                lower.startsWith("forget that ") -> trimmed.substring("forget that ".length)
                lower.startsWith("forget ") -> trimmed.substring("forget ".length)
                else -> trimmed
            }
            val keyword = rawKeyword.trim().trimEnd('.', '!', '?', ',')
            if (keyword.isNotBlank()) {
                val count = memoryRepository.deleteByKeyword(keyword)
                return MemoryCommandResult.Forgotten(keyword, count)
            }
        }

        // 5. "Remember that..." / "Remember: ..."
        if (lower.startsWith("remember that ") || lower.startsWith("remember: ")) {
            val rawFact = when {
                lower.startsWith("remember that ") -> trimmed.substring("remember that ".length).trim()
                lower.startsWith("remember: ") -> trimmed.substring("remember: ".length).trim()
                else -> trimmed
            }
            if (rawFact.isNotBlank()) {
                val category = categorizeFact(rawFact)
                memoryRepository.saveMemory(content = rawFact, category = category)
                return MemoryCommandResult.Saved(rawFact, category)
            }
        }

        // 6. Implicit Name / Identity Statement (e.g. "My name is Shubh.", "My name is Rahul", "Call me Shubh", "I am Shubh")
        val nameStatementRegex = Regex("""^(?:hi|hello|hey)?\s*(?:my name is|call me|i am|i'm)\s+([A-Za-z]+)(?:\.|\!|\,)?$""", RegexOption.IGNORE_CASE)
        val nameMatch = nameStatementRegex.find(trimmed)
        if (nameMatch != null) {
            val extractedName = nameMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            memoryRepository.saveName(extractedName)
            return MemoryCommandResult.Saved("Name: $extractedName", MemoryCategory.PROFILE)
        }

        return MemoryCommandResult.NotACommand
    }

    suspend fun getMemoryContextPrompt(): String {
        val memories = memoryRepository.getMemoriesSnapshot()
        if (memories.isEmpty()) return ""

        val lines = memories.map { mem ->
            val c = mem.content.trim()
            when {
                c.startsWith("my name is ", ignoreCase = true) -> {
                    val name = c.substring("my name is ".length).trim().trimEnd('.', '!', ',').replaceFirstChar { it.uppercase() }
                    "Name: $name"
                }
                c.startsWith("name:", ignoreCase = true) -> {
                    "Name: " + c.substring("name:".length).trim().replaceFirstChar { it.uppercase() }
                }
                else -> c
            }
        }

        val builder = StringBuilder("Persistent User Profile & Long-Term Preferences (from local memory):\n")
        builder.append("Information in LOCAL USER MEMORY represents facts explicitly stored by the user. Use these facts when relevant. Do not claim not to know information that is explicitly present in LOCAL USER MEMORY.\n\n")
        builder.append("USER MEMORY:\n")
        lines.forEach { line ->
            builder.append(line).append("\n")
        }
        return builder.toString().trim()
    }

    private fun categorizeFact(fact: String): MemoryCategory {
        val lower = fact.lowercase()
        return when {
            lower.contains("my name is") || lower.contains("name is") || lower.contains("call me") ||
                (lower.contains("i am") || lower.contains("i'm")) && (lower.contains("year") || lower.contains("student") || lower.contains("engineer")) ->
                MemoryCategory.PROFILE

            lower.contains("exam") || lower.contains("studying") || lower.contains("course") || lower.contains("semester") || lower.contains("college") || lower.contains("syllabus") ->
                MemoryCategory.ACADEMIC

            lower.contains("prefer") || lower.contains("like") || lower.contains("don't like") || lower.contains("concise") || lower.contains("detailed") || lower.contains("bullet") ->
                MemoryCategory.PREFERENCE

            lower.contains("goal") || lower.contains("aim") || lower.contains("target") || lower.contains("want to learn") || lower.contains("score") ->
                MemoryCategory.GOAL

            else -> MemoryCategory.GENERAL
        }
    }
}
