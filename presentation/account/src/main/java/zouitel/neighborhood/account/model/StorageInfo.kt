package zouitel.neighborhood.account.model

data class StorageInfo(
    val notesBytes: Long = 0L,
    val mediaBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val quotaBytes: Long = 5L * 1024 * 1024 * 1024
) {
    val usedBytes: Long get() = notesBytes + mediaBytes + cacheBytes
    val freeBytes: Long get() = (quotaBytes - usedBytes).coerceAtLeast(0L)
}