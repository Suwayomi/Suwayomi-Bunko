package dev.bunko.domain.user.interactor

import dev.bunko.domain.user.service.UserPreferences
import dev.bunko.domain.user.service.UserRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.singleOrNull
import me.tatarka.inject.annotations.Inject

@Inject
class UserLoginSimple(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
) {
    suspend fun await(
        username: String,
        password: String,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(username, password)
        .catch {
            onError(it)
            log.warn(it) { "Failed to login with user $username" }
        }
        .singleOrNull()

    fun asFlow(
        username: String,
        password: String,
    ) = userRepository.loginSimple(username, password)
        .onEach {
            userPreferences.simpleSession().set(it)
            userPreferences.simpleUsername().set(username)
            userPreferences.simplePassword().set(password)
        }

    companion object {
        private val log = logging()
    }
}
