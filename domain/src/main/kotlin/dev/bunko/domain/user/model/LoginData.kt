package dev.bunko.domain.user.model

data class LoginData(
    val refreshToken: String,
    val accessToken: String,
)
