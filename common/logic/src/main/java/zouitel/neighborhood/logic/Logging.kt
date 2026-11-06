package zouitel.neighborhood.logic

private const val MAX_RECENT_LOGS = 100

object Logging {
    private val lock = Any()
    private val recentLogs = ArrayDeque<String>()

    fun log(tag: String, message: String) {
        synchronized(lock) {
            recentLogs.addFirst("$tag: $message")
            if (recentLogs.size > MAX_RECENT_LOGS) recentLogs.removeLast()
        }
    }

    /** Returns an immutable snapshot, not the mutable backing collection. */
    fun getRecentLogs(): List<String> = synchronized(lock) {
        recentLogs.toList()
    }
}
