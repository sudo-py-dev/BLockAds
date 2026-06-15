package com.blockads.app.core.data.blocklist

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BlocklistRepository"
private const val CACHE_FILE_NAME = "blocklist_cache.txt"
private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

@Singleton
class BlocklistRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val okHttpClient: OkHttpClient,
    ) {
        @Volatile private var blockedDomains: HashSet<String> = HashSet()

        @Volatile private var isLoaded = false

        val count: Int get() = blockedDomains.size

        fun isDomainBlocked(domain: String): Boolean {
            val lower = domain.lowercase()
            if (blockedDomains.contains(lower)) return true
            
            var dot = lower.indexOf('.')
            while (dot != -1 && dot < lower.length - 1) {
                val parent = lower.substring(dot + 1)
                if (blockedDomains.contains(parent)) return true
                dot = lower.indexOf('.', dot + 1)
            }
            return false
        }

        suspend fun ensureLoaded(source: BlocklistSource) {
            if (!isLoaded || blockedDomains.isEmpty()) refresh(source)
        }

        suspend fun refresh(source: BlocklistSource): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    when {
                        source.remoteUrl != null -> downloadAndCache(source.remoteUrl!!)
                        else -> loadBundled()
                    }
                    isLoaded = true
                    Result.success(Unit)
                }.getOrElse { e ->
                    Log.e(TAG, "Refresh failed, trying cache", e)
                    runCatching { 
                        loadFromCache()
                        isLoaded = true
                        Result.success(Unit)
                    }.getOrDefault(Result.failure(e))
                }
            }

        private fun loadBundled() {
            val set = HashSet<String>(150_000)
            context.assets.open("hosts_bundled.txt").bufferedReader().use { reader ->
                HostsParser.parseLines(reader.lineSequence()).forEach { set.add(it) }
            }
            blockedDomains = set
            Log.d(TAG, "Loaded ${set.size} domains from bundled asset")
        }

        private fun downloadAndCache(url: String) {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                
                val tempFile = File(context.filesDir, "${CACHE_FILE_NAME}.tmp")
                val set = HashSet<String>(150_000)
                
                runCatching {
                    tempFile.bufferedWriter().use { writer ->
                        body.source().inputStream().bufferedReader().use { reader ->
                            HostsParser.parseLines(reader.lineSequence()).forEach { domain ->
                                if (set.add(domain)) {
                                    writer.write(domain)
                                    writer.newLine()
                                }
                            }
                        }
                    }
                    val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                    if (tempFile.renameTo(cacheFile)) {
                        blockedDomains = set
                        Log.d(TAG, "Downloaded and cached ${set.size} domains")
                    } else {
                        throw IOException("Failed to rename temp cache file")
                    }
                }.onFailure { e ->
                    tempFile.delete()
                    throw e
                }
            }
        }

        private fun loadFromCache() {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (!cacheFile.exists()) return
            val set = HashSet<String>(150_000)
            cacheFile.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    val domain = line.trim()
                    if (domain.isNotEmpty()) set.add(domain)
                }
            }
            blockedDomains = set
            Log.d(TAG, "Loaded ${set.size} domains from disk cache")
        }

        fun reset() {
            blockedDomains = HashSet()
            isLoaded = false
        }
    }
