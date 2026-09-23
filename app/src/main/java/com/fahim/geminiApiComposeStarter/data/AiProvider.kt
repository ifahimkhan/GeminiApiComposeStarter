package com.fahim.geminiApiComposeStarter.data

enum class AiProvider(val displayName: String, val modelId: String, val keyProperty: String) {
    GEMINI("Gemini", "gemini-3.6-flash", "GEMINI_API_KEY"),
    OPENAI("OpenAI", "gpt-4.1-mini", "OPENAI_API_KEY"),
    DEEPSEEK("DeepSeek", "deepseek-flash", "DEEPSEEK_API_KEY")
}
