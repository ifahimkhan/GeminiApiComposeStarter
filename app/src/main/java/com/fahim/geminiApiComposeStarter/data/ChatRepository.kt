package com.fahim.geminiApiComposeStarter.data

interface ChatRepository {
    suspend fun generateText(provider: AiProvider, prompt: String): Result<String>
}

class ProviderChatRepository(private val vault: ApiKeyVault) : ChatRepository {
    private val gemini by lazy { GeminiRepositoryImpl(vault) }
    private val compatible by lazy { OpenAiCompatibleRepository(vault) }

    override suspend fun generateText(provider: AiProvider, prompt: String): Result<String> =
        when (provider) {
            AiProvider.GEMINI -> gemini.generateText(prompt)
            AiProvider.OPENAI, AiProvider.DEEPSEEK -> compatible.generateText(provider, prompt)
        }
}
