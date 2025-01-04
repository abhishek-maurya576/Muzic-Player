package com.org.muzicplayer

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongCache {
    private val memoryCache = LruCache<Long, Song>(100)
    private val metadataCache = LruCache<Long, SongMetadata>(100)

    suspend fun getCachedSong(id: Long): Song? = withContext(Dispatchers.IO) {
        return@withContext memoryCache.get(id)
    }

    suspend fun cacheSong(song: Song) = withContext(Dispatchers.IO) {
        memoryCache.put(song.id, song)
    }

    suspend fun getCachedMetadata(id: Long): SongMetadata? = withContext(Dispatchers.IO) {
        return@withContext metadataCache.get(id)
    }

    suspend fun cacheMetadata(id: Long, metadata: SongMetadata) = withContext(Dispatchers.IO) {
        metadataCache.put(id, metadata)
    }

    fun clear() {
        memoryCache.evictAll()
        metadataCache.evictAll()
    }

    data class SongMetadata(
        val duration: Long,
        val bitrate: Int,
        val sampleRate: Int
    )

    companion object {
        @Volatile
        private var instance: SongCache? = null

        fun getInstance(): SongCache {
            return instance ?: synchronized(this) {
                instance ?: SongCache().also { instance = it }
            }
        }
    }
}
