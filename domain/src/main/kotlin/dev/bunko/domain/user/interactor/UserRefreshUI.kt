package dev.bunko.domain.user.interactor

import dev.bunko.domain.user.service.UserPreferences
import dev.bunko.domain.user.service.UserRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Inject
class UserRefreshUI(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
) {
    suspend fun await(
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow()
        .catch {
            onError(it)
            log.warn(it) { "Failed to refresh user" }
        }
        .singleOrNull()

    fun asFlow() = userRepository.refreshUI(userPreferences.uiRefreshToken().get())
        .onEach {
            userPreferences.uiAccessToken().set(it)
        }

    suspend fun recoverUI(staleToken: String): String? = mutex.withLock {
        val current = userPreferences.uiAccessToken().get()
        if (current.isNotEmpty() && current != staleToken) return@withLock current
        if (userPreferences.uiRefreshToken().get().isEmpty()) return@withLock null
        if (recentlyFailed(staleToken)) return@withLock null
        await() ?: remember(staleToken)
    }

    suspend fun recoverSimple(staleSession: String): String? = mutex.withLock {
        val current = userPreferences.simpleSession().get()
        if (current.isNotEmpty() && current != staleSession) return@withLock current

        val username = userPreferences.simpleUsername().get()
        if (username.isEmpty()) return@withLock null
        if (recentlyFailed(staleSession)) return@withLock null

        log.info { "Session expired, logging in again" }
        userRepository.loginSimple(username, userPreferences.simplePassword().get())
            .onEach { userPreferences.simpleSession().set(it) }
            .catch { log.warn(it) { "Failed to renew simple_login session" } }
            .singleOrNull()
            ?: remember(staleSession)
    }

    private fun recentlyFailed(credential: String): Boolean {
        val failure = lastFailure ?: return false
        return failure.credential == credential && failure.at.elapsedNow() < FAILURE_MEMORY
    }

    private fun remember(credential: String): String? {
        lastFailure = Failure(credential, TimeSource.Monotonic.markNow())
        return null
    }

    private class Failure(val credential: String, val at: TimeMark)

    companion object {
        private var lastFailure: Failure? = null
        private val FAILURE_MEMORY = 30.seconds
        private val log = logging()
        private val mutex = Mutex()
    }
}
