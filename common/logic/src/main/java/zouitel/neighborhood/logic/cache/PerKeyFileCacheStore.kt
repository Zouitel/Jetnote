package zouitel.neighborhood.logic.cache

import android.util.AtomicFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PerKeyFileCacheStore<K : Any, V : Any>(
    private val dir: File,
    private val keyCodec: KeyCodec<K>,
    private val valueSerializer: KSerializer<V>,
    private val json: Json = Json { prettyPrint = false; ignoreUnknownKeys = true }
) : CacheStore<K, V> {
    private val lock = ReentrantLock()
    private val ext = ".json"
    private val entrySerializer = cacheEntrySerializer(valueSerializer)

    override fun loadEntry(key: K): CacheEntry<V>? = lock.withLock {
        val file = fileFor(key)
        val text = readAtomicText(file) ?: return@withLock null
        decodeEntry(text)
    }

    override fun saveEntry(key: K, entry: CacheEntry<V>) = lock.withLock {
        val file = fileFor(key)
        ensureParentDir(file)
        atomicWrite(file, json.encodeToString(entrySerializer, entry))
    }

    override fun remove(key: K) = lock.withLock {
        AtomicFile(fileFor(key)).delete()
    }

    override fun clear() = lock.withLock {
        if (!dir.exists()) return@withLock
        dir.listFiles { file -> file.isFile && file.name.endsWith(ext) }
            ?.forEach { AtomicFile(it).delete() }
    }

    override fun loadAllEntries(): Map<K, CacheEntry<V>> = lock.withLock {
        if (!dir.exists()) return@withLock emptyMap()
        val result = LinkedHashMap<K, CacheEntry<V>>()
        dir.listFiles { file -> file.isFile && file.name.endsWith(ext) }?.forEach { file ->
            val key = keyCodec.fromFileName(file.name.removeSuffix(ext)) ?: return@forEach
            val text = readAtomicText(file) ?: return@forEach
            decodeEntry(text)?.let { result[key] = it }
        }
        result
    }

    override fun keys(): Set<K> = lock.withLock {
        if (!dir.exists()) return@withLock emptySet()
        buildSet {
            dir.listFiles { file -> file.isFile && file.name.endsWith(ext) }?.forEach { file ->
                keyCodec.fromFileName(file.name.removeSuffix(ext))?.let(::add)
            }
        }
    }

    private fun decodeEntry(text: String): CacheEntry<V>? {
        return runCatching { json.decodeFromString(entrySerializer, text) }.getOrElse {
            runCatching { json.decodeFromString(valueSerializer, text) }
                .getOrNull()?.let { CacheEntry(value = it, expiresAt = null) }
        }
    }

    private fun fileFor(key: K): File = File(dir, keyCodec.toFileName(key) + ext)
}
