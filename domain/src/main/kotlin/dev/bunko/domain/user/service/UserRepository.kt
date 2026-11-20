package dev.bunko.domain.user.service

import dev.bunko.domain.user.model.LoginData
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun loginUI(username: String, password: String): Flow<LoginData>

    fun refreshUI(refreshToken: String): Flow<String>

    fun loginSimple(username: String, password: String): Flow<String>
}
