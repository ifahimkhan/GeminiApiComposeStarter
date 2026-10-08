package com.n149.geminichat.data

import android.content.Context
import androidx.room.Room
import com.n149.geminichat.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module providing all data-layer singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase =
        Room.databaseBuilder(context, ChatDatabase::class.java, "chat_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMessageDao(db: ChatDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideFeedbackDao(db: ChatDatabase): FeedbackDao = db.feedbackDao()

    @Provides
    @Singleton
    fun provideBuildConfigApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides
    @Singleton
    fun provideGeminiRepository(
        keystoreManager: KeystoreManager,
        buildConfigApiKey: String
    ): GeminiRepositoryInterface = GeminiRepository(keystoreManager, buildConfigApiKey)
}
