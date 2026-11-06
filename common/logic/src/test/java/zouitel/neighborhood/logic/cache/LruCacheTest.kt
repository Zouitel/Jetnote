package zouitel.neighborhood.logic.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LruCacheTest {
    @Test
    fun evictsLeastRecentlyUsedEntry() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache(2, store)
        cache.put("a", "A")
        cache.put("b", "B")

        assertEquals("A", cache.get("a"))
        cache.put("c", "C")

        assertTrue(cache.containsKey("a"))
        assertTrue(cache.containsKey("c"))
        assertFalse(cache.containsKey("b"))
        assertEquals(setOf("a", "c"), cache.keysInMemory())
    }

    @Test
    fun rejectsNonPositiveCapacity() {
        val store = InMemoryCacheStore<String, String>()
        assertThrows(IllegalArgumentException::class.java) {
            LruCache(0, store)
        }
    }

    @Test
    fun expiredEntryIsRemovedFromMemoryAndStore() {
        val store = InMemoryCacheStore<String, String>()
        val cache = LruCache(2, store)
        cache.put("key", "value", 0)

        assertNull(cache.get("key"))
        assertFalse(store.entries.containsKey("key"))
        assertEquals(0, cache.size())
    }

    private class InMemoryCacheStore<K : Any, V> : CacheStore<K, V> {
        val entries = linkedMapOf<K, CacheEntry<V>>()

        override fun loadEntry(key: K): CacheEntry<V>? = entries[key]
        override fun saveEntry(key: K, entry: CacheEntry<V>) {
            entries[key] = entry
        }
        override fun remove(key: K) {
            entries.remove(key)
        }
        override fun clear() {
            entries.clear()
        }
        override fun loadAllEntries(): Map<K, CacheEntry<V>> = entries.toMap()
        override fun keys(): Set<K> = entries.keys.toSet()
    }
}
