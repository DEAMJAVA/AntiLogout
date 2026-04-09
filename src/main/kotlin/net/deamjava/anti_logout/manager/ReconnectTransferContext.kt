package net.deamjava.anti_logout.manager

object ReconnectTransferContext {
    private val allowRemovalTL = ThreadLocal.withInitial { false }
    private val reconnectRemovalAllowedUntilMs = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long>()
    private const val RECONNECT_ALLOW_WINDOW_MS = 15_000L

    fun isAllowingLingeringRemoval(uuid: java.util.UUID? = null): Boolean {
        if (allowRemovalTL.get()) return true
        if (uuid == null) return false

        val now = System.currentTimeMillis()
        val expires = reconnectRemovalAllowedUntilMs[uuid] ?: return false
        if (expires < now) {
            reconnectRemovalAllowedUntilMs.remove(uuid, expires)
            return false
        }
        return true
    }

    fun markReconnectForUuid(uuid: java.util.UUID) {
        reconnectRemovalAllowedUntilMs[uuid] = System.currentTimeMillis() + RECONNECT_ALLOW_WINDOW_MS
    }

    fun clearReconnectForUuid(uuid: java.util.UUID) {
        reconnectRemovalAllowedUntilMs.remove(uuid)
    }

    fun <T> withAllowedLingeringRemoval(block: () -> T): T {
        val prev = allowRemovalTL.get()
        allowRemovalTL.set(true)
        return try {
            block()
        } finally {
            allowRemovalTL.set(prev)
        }
    }
}
