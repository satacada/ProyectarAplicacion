package com.omnicast.receiver.directstream

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Administrador de caché inteligente para ExoPlayer en Android TV.
 * Almacena en disco los fragmentos descargados (HLS/MP4) para que
 * fluctuaciones de red o pérdidas de paquetes no causen pausas ni tirones.
 */
@OptIn(UnstableApi::class)
object TvMediaCacheManager {
    private const val TAG = "TvMediaCacheManager"
    private const val MAX_CACHE_SIZE_BYTES = 400L * 1024L * 1024L // 400 MB de buffer en almacenamiento

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: run {
            val cacheDir = File(context.cacheDir, "tv_media_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            val databaseProvider = StandaloneDatabaseProvider(context)
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCache = it
                Log.i(TAG, "Caché de medios en TV inicializada (${MAX_CACHE_SIZE_BYTES / (1024 * 1024)} MB)")
            }
        }
    }

    fun buildCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory
    ): DataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun release() {
        try {
            simpleCache?.release()
            simpleCache = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error liberando SimpleCache: ${e.message}")
        }
    }
}
