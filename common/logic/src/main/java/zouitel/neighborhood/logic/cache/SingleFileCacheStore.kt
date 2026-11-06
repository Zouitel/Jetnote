package zouitel.neighborhood.logic.cache

import android.util.AtomicFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SingleFileCacheStore<K : Any, V : Any>(
    private val file: File,
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
    private val json: Json = Json { prettyPrint = false; ignoreUnknownKeys = true; allowStructuredMapKeys = true }
) : CacheStore<K, V> {
    private val lock = ReentrantLock()
    private val entrySerializer = cacheEntrySerializer(valueSerializer)
    private val mapEntrySerializer = MapSerializer(keySerializer, entrySerializer)

    override fun loadEntry(key: K): CacheEntry<V>? = lock.withLock {
        safeReadMap()[key]
    }

    override fun saveEntry(key: K, entry: CacheEntry<V>) = lock.withLock {
        val all = safeReadMap().toMutableMap()
        all[key] = entry
        safeWriteMap(all)
    }

    override fun remove(key: K) = lock.withLock {
        val all = safeReadMap().toMutableMap()
        if (all.remove(key) != null) safeWriteMap(all)
    }

    override fun clear() = lock.withLock {
        AtomicFile(file).delete()
    }

    override fun loadAllEntries(): Map<K, CacheEntry<V>> = lock.withLock { safeReadMap() }

    override fun keys(): Set<K> = lock.withLock { safeReadMap().keys }

    private fun safeReadMap(): Map<K, CacheEntry<V>> {
        val text = try {
            readAtomicText(file) ?: return emptyMap()
        } catch (failure: Exception) {
            throw IOException("Unable to read cache file: $file", failure)
        }
        if (text.isBlank()) return emptyMap()

        try {
            return json.decodeFromString(mapEntrySerializer, text)
        } catch (currentFormatFailure: Exception) {
            try {
                val legacyMap = json.decodeFromString(
                    MapSerializer(keySerializer, valueSerializer),
                    text
                )
                return legacyMap.mapValues { CacheEntry(value = it.value, expiresAt = null) }
            } catch (legacyFormatFailure: Exception) {
                legacyFormatFailure.addSuppressed(currentFormatFailure)
                throw IOException(
                    "Cache file is corrupt or uses an unsupported format; refusing to overwrite it: $file",
                    legacyFormatFailure
                )
            }
        }
    }

    private fun safeWriteMap(map: Map<K, CacheEntry<V>>) {
        ensureParentDir(file)
        val text = json.encodeToString(mapEntrySerializer, map)
        atomicWrite(file, text)
    }
}
