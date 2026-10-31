package com.lucid.alarm.service

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

object NotificationRateLimiter {
    const val MIN_GLOBAL_NOTIFY_GAP_MS = 230L
    private val mutex = Mutex()
    @Volatile private var lastPostElapsedRealtime = 0L

    suspend fun acquireSlot(minGapMs: Long = MIN_GLOBAL_NOTIFY_GAP_MS) {
        mutex.lock()
        try {
            val gap = maxOf(minGapMs, MIN_GLOBAL_NOTIFY_GAP_MS)
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastPostElapsedRealtime
            if (elapsed < gap) {
                delay(gap - elapsed)
            }
            lastPostElapsedRealtime = SystemClock.elapsedRealtime()
        } finally {
            mutex.unlock()
        }
    }

    fun markPostedFromMainThread() {
        lastPostElapsedRealtime = SystemClock.elapsedRealtime()
    }
}
