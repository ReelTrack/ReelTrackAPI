package com.example.utils

import com.example.models.User
import com.example.models.UserResponse

fun User.toResponse(): UserResponse {
    return UserResponse(
        id = this.id!!,
        username = this.username,
        email = this.email,
        role = this.role,
        createdAt = this.createdAt!!,
        updatedAt = this.updatedAt!!,
        isBanned = this.isBanned
    )
}
