package com.example.models

import com.example.models.enums.UserRole
import com.example.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class User(
    val id: Int? = null,
    val username: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole = UserRole.USER,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val isBanned: Boolean = false
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val role: UserRole,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val isBanned: Boolean
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String? = null,
    val passwordHash: String? = null,
    val role: UserRole = UserRole.USER
) {
    fun getPasswordValue(): String {
        return password ?: passwordHash ?: throw IllegalArgumentException("Either 'password' or 'passwordHash' must be provided")
    }
}

@Serializable
data class UpdateUserRequest(
    val username: String,
    val email: String,
    val password: String? = null,
    val passwordHash: String? = null,
    val role: UserRole,
    val isBanned: Boolean = false
) {
    fun getPasswordValue(): String? {
        return password ?: passwordHash
    }
}

@Serializable
data class ApiResponse(
    val message: String,
    val id: Int? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)
