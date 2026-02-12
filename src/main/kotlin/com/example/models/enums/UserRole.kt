package com.example.models.enums

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    USER, ADMIN, MODERATOR;

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.uppercase()) {
                "USER" -> USER
                "ADMIN" -> ADMIN
                "MODERATOR" -> MODERATOR
                else -> USER
            }
        }
    }
}
