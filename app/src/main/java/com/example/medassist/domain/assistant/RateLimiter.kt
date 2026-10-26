package com.example.medassist.domain.assistant

/** Allows at most [maxRequests] in any [windowMillis] period. Not thread-safe: use from the main thread. */
class RateLimiter(
    private val maxRequests: Int = AssistantConfig.RATE_LIMIT_COUNT,
    private val windowMillis: Long = AssistantConfig.RATE_LIMIT_WINDOW_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val times = ArrayDeque<Long>()

    fun tryAcquire(): Boolean {
        val now = clock()
        while (times.isNotEmpty() && now - times.first() >= windowMillis) times.removeFirst()
        if (times.size >= maxRequests) return false
        times.addLast(now)
        return true
    }

    /** Gives a slot back, for requests that never reached the server (for example when offline). */
    fun release() {
        if (times.isNotEmpty()) times.removeLast()
    }
}
