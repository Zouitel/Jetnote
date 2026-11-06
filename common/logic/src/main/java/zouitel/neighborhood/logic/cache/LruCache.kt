package zouitel.neighborhood.logic.cache

import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A thread-safe, write-through LRU cache.
 *
 * Store operations are serialized with in-memory mutations so a slow read cannot
 * overwrite a newer value written by another thread.
 */
class LruCache<K, V>(
    private val capacity: Int,
    private val store: CacheStore<K, V>,
    private val deleteOnEvict: Boolean = false,
    preloadFromStore: Boolean = false,
    private val expireAfterWriteMillis: Long? = null
) where K : Any {
    private val lock = ReentrantLock()
    private val map = LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true)

    init {
        require(capacity > 0) { "capacity must be greater than zero" }
        require(expireAfterWriteMillis == null || expireAfterWriteMillis >= 0) {
            "expireAfterWriteMillis must not be negative"
        }

        if (preloadFromStore) {
            runCatching {
                lock.withLock {
                    val currentTime = now()
                    store.loadAllEntries().forEach { (key, entry) ->
                        if (entry.isExpired(currentTime)) {
                            runCatching { store.remove(key) }
                        } else {
                            map[key] = entry
                            evictOverflow()
                        }
                    }
                }
            }
        }
    }

    fun get(key: K): V? = lock.withLock {
        map[key]?.let { entry ->
            if (!entry.isExpired(now())) return@withLock entry.value
            map.remove(key)
            if (deleteOnEvict) runCatching { store.remove(key) }
        }

        val entry = runCatching { store.loadEntry(key) }.getOrNull() ?: return@withLock null
        if (entry.isExpired(now())) {
            runCatching { store.remove(key) }
            return@withLock null
        }

        map[key] = entry
        evictOverflow()
        entry.value
    }

    fun put(key: K, value: V) = put(key, value, expireAfterWriteMillis)

    fun put(key: K, value: V, ttlMillis: Long?) {
        require(ttlMillis == null || ttlMillis >= 0) { "ttlMillis must not be negative" }
        val entry = CacheEntry(value = value, expiresAt = ttlMillis?.let { now() + it })
        lock.withLock {
            map[key] = entry
            runCatching { store.saveEntry(key, entry) }
            evictOverflow()
        }
    }

    fun remove(key: K) = lock.withLock {
        map.remove(key)
        runCatching { store.remove(key) }
        Unit
    }

    fun clear() = lock.withLock {
        map.clear()
        runCatching { store.clear() }
        Unit
    }

    fun containsKey(key: K): Boolean = lock.withLock {
        map[key]?.let { entry ->
            if (!entry.isExpired(now())) return@withLock true
            map.remove(key)
        }

        val entry = runCatching { store.loadEntry(key) }.getOrNull() ?: return@withLock false
        if (entry.isExpired(now())) {
            runCatching { store.remove(key) }
            false
        } else {
            true
        }
    }

    /** Number of currently resident (in-memory) entries. */
    fun size(): Int = lock.withLock {
        removeExpiredEntries()
        map.size
    }

    fun keysInMemory(): Set<K> = lock.withLock {
        removeExpiredEntries()
        map.keys.toSet()
    }

    private fun removeExpiredEntries() {
        val currentTime = now()
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired(currentTime)) {
                val key = entry.key
                iterator.remove()
                if (deleteOnEvict) runCatching { store.remove(key) }
            }
        }
    }

    private fun evictOverflow() {
        while (map.size > capacity) {
            val iterator = map.entries.iterator()
            if (!iterator.hasNext()) return
            val eldestKey = iterator.next().key
            iterator.remove()
            if (deleteOnEvict) runCatching { store.remove(eldestKey) }
        }
    }
}

private fun now(): Long = System.currentTimeMillis()
