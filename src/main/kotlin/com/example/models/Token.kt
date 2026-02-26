package com.example.models

import com.example.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Token(
    val id: Int? = null,
    val userId: Int,
    val token: String,
    val refreshToken: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val expiresAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val refreshExpiresAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val isRevoked: Boolean = false
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)