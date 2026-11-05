package zouitel.neighborhood.account.model

/**
 * Device-local app storage. This is deliberately not presented as a Google Drive
 * quota: Drive storage limits belong to the signed-in Google account and require
 * querying Drive's storage metadata separately.
 */
data class StorageInfo(
    val notesBytes: Long = 0L,
    val mediaBytes: Long = 0L,
    val cacheBytes: Long = 0L
) {
    val usedBytes: Long
        get() = notesBytes + mediaBytes + cacheBytes
}
