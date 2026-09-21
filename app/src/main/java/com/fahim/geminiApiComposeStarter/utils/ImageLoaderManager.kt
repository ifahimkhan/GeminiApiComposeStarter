package com.fahim.geminiApiComposeStarter.utils

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared helpers for the Coil / Glide demo screens: sample URLs, preloading, cache clearing. */
object ImageLoaderManager {

    val sampleUrls: List<String> = (10..17).map { "https://picsum.photos/id/$it/800/600" }

    /** Warms Coil's disk cache (and memory cache) without any view attached. */
    fun preloadWithCoil(context: Context, urls: List<String> = sampleUrls) {
        val loader = SingletonImageLoader.get(context)
        urls.forEach { url ->
            loader.enqueue(ImageRequest.Builder(context).data(url).build())
        }
    }

    /** Warms Glide's disk cache the same way. */
    fun preloadWithGlide(context: Context, urls: List<String> = sampleUrls) {
        urls.forEach { url -> Glide.with(context).load(url).preload() }
    }

    /** Clears memory + disk caches for both libraries. Call from the main thread. */
    suspend fun clearCaches(context: Context) {
        val coil = SingletonImageLoader.get(context)
        coil.memoryCache?.clear()
        coil.diskCache?.clear()
        Glide.get(context).clearMemory()                 // must run on the main thread
        withContext(Dispatchers.IO) {
            Glide.get(context).clearDiskCache()          // must run off the main thread
        }
    }
}
