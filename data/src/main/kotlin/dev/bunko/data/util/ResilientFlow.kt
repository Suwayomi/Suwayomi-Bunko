package dev.bunko.data.util

import com.diamondedge.logging.logging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

private val log = logging("ResilientFlow")

private const val BASE_DELAY_MS = 1_000L
private const val MAX_DELAY_MS = 20_000L

internal fun <T> resilient(
    name: String,
    onReconnecting: () -> Unit = {},
    source: () -> Flow<T>,
): Flow<T> = flow {
    var attempt = 0
    while (true) {
        source()
            .catch { log.warn(it) { "$name dropped" } }
            .collect {
                attempt = 0
                emit(it)
            }
        onReconnecting()
        val delayMs = (BASE_DELAY_MS shl attempt.coerceAtMost(5)).coerceAtMost(MAX_DELAY_MS)
        attempt++
        log.info { "$name reconnecting in ${delayMs}ms" }
        delay(delayMs + Random.nextLong(delayMs / 5))
    }
}
