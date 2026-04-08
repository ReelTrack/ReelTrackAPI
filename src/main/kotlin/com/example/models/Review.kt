package com.example.models

import com.example.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

// ============================================================
// REVIEW
// ============================================================

@Serializable
data class Review(
    val id: Int? = null,
    val userId: Int,
    val contentId: Int,
    val rating: Short? = null,
    val body: String? = null,
    val isSpoiler: Boolean = false,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null
)

// Полный ответ с лайками и комментариями
@Serializable
data class ReviewResponse(
    val id: Int,
    val userId: Int,
    val username: String,
    val contentId: Int,
    val rating: Short? = null,
    val body: String? = null,
    val isSpoiler: Boolean = false,
    val likesCount: Int = 0,
    val likes: List<ReviewLike> = emptyList(),
    val comments: List<ReviewCommentResponse> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime
)

@Serializable
data class ReviewLike(
    val userId: Int,
    val username: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)

@Serializable
data class CreateReviewRequest(
    val rating: Short? = null,
    val body: String? = null,
    val isSpoiler: Boolean = false
)

@Serializable
data class UpdateReviewRequest(
    val rating: Short? = null,
    val body: String? = null,
    val isSpoiler: Boolean = false
)

// ============================================================
// REVIEW COMMENT
// ============================================================

@Serializable
data class ReviewComment(
    val id: Int? = null,
    val reviewId: Int,
    val userId: Int,
    val parentId: Int? = null,
    val body: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class ReviewCommentResponse(
    val id: Int,
    val reviewId: Int,
    val userId: Int,
    val username: String,
    val parentId: Int? = null,
    val body: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime
)

@Serializable
data class CreateCommentRequest(
    val body: String,
    val parentId: Int? = null
)

@Serializable
data class UpdateCommentRequest(
    val body: String
)

// ============================================================
// LIKE RESPONSE
// ============================================================

@Serializable
data class LikeResponse(
    val liked: Boolean,
    val likesCount: Int
)