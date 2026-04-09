package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.models.*
import com.example.plugins.getUserId
import com.example.plugins.isModeratorOrAdmin
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reviewRoutes() {

    // ============================================================
    // REVIEWS — вложены под /api/content/{contentId}/reviews
    // ============================================================

    route("/api/content/{contentId}/reviews") {

        // GET — публичный
        get {
            val contentId = call.parameters["contentId"]?.toIntOrNull()
            if (contentId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                return@get
            }

            val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
            val reviews = reviewService.getReviewsByContent(contentId)
            call.respond(HttpStatusCode.OK, reviews)
        }

        authenticate("auth-jwt") {

            // POST — авторизованный пользователь
            post {
                val contentId = call.parameters["contentId"]?.toIntOrNull()
                if (contentId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                    return@post
                }

                val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                val userId = call.getUserId()
                val request = call.receive<CreateReviewRequest>()

                try {
                    val id = reviewService.createReview(userId, contentId, request)
                    call.respond(HttpStatusCode.Created, ApiResponse("Review created successfully", id))
                } catch (e: Exception) {
                    val msg = e.message?.lowercase().orEmpty()
                    val status = if (msg.contains("duplicate") || msg.contains("unique"))
                        HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                    call.respond(status, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }
    }

    // ============================================================
    // ОТДЕЛЬНЫЙ ОТЗЫВ — /api/reviews/{id}
    // ============================================================

    route("/api/reviews/{id}") {

        // GET — публичный
        get {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                return@get
            }

            val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
            val review = reviewService.getReviewById(id)
            if (review != null) {
                call.respond(HttpStatusCode.OK, review)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Review not found"))
            }
        }

        authenticate("auth-jwt") {

            // PUT — свой отзыв или ADMIN/MODERATOR
            put {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                    return@put
                }

                val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                val existing = reviewService.getReviewById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Review not found"))
                    return@put
                }

                val currentUserId = call.getUserId()
                if (existing.userId != currentUserId && !call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. You can only edit your own reviews."))
                    return@put
                }

                val request = call.receive<UpdateReviewRequest>()
                try {
                    reviewService.updateReview(id, request)
                    call.respond(HttpStatusCode.OK, ApiResponse("Review updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // DELETE — свой отзыв или ADMIN/MODERATOR
            delete {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                    return@delete
                }

                val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                val existing = reviewService.getReviewById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Review not found"))
                    return@delete
                }

                val currentUserId = call.getUserId()
                if (existing.userId != currentUserId && !call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. You can only delete your own reviews."))
                    return@delete
                }

                try {
                    reviewService.deleteReview(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("Review deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // POST /api/reviews/{id}/like — toggle лайка
            post("/like") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                    return@post
                }

                val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                if (reviewService.getReviewById(id) == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Review not found"))
                    return@post
                }

                val currentUserId = call.getUserId()
                try {
                    val result = reviewService.toggleLike(currentUserId, id)
                    call.respond(HttpStatusCode.OK, result)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // ============================================================
            // COMMENTS — /api/reviews/{id}/comments
            // ============================================================

            route("/comments") {
                // POST — авторизованный
                post {
                    val reviewId = call.parameters["id"]?.toIntOrNull()
                    if (reviewId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                        return@post
                    }

                    val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                    if (reviewService.getReviewById(reviewId) == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Review not found"))
                        return@post
                    }

                    val userId = call.getUserId()
                    val request = call.receive<CreateCommentRequest>()
                    try {
                        val id = reviewService.createComment(userId, reviewId, request)
                        call.respond(HttpStatusCode.Created, ApiResponse("Comment created successfully", id))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    // ============================================================
    // GET лайков и комментариев — публичные эндпоинты
    // ============================================================

    // GET /api/reviews/{id}/likes — публичный список лайков
    route("/api/reviews/{id}/likes") {
        get {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                return@get
            }

            val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
            if (reviewService.getReviewById(id) == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Review not found"))
                return@get
            }

            val likes = reviewService.getLikesByReview(id)
            call.respond(HttpStatusCode.OK, likes)
        }
    }

    // GET /api/reviews/{id}/comments — публичный
    route("/api/reviews/{id}/comments") {
        get {
            val reviewId = call.parameters["id"]?.toIntOrNull()
            if (reviewId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid review ID"))
                return@get
            }

            val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
            val comments = reviewService.getCommentsByReview(reviewId)
            call.respond(HttpStatusCode.OK, comments)
        }
    }

    // ============================================================
    // ОТДЕЛЬНЫЙ КОММЕНТАРИЙ — /api/comments/{id}
    // ============================================================

    route("/api/comments/{id}") {

        authenticate("auth-jwt") {

            // PUT — свой комментарий или ADMIN/MODERATOR
            put {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid comment ID"))
                    return@put
                }

                val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                val existing = reviewService.getCommentById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Comment not found"))
                    return@put
                }

                val currentUserId = call.getUserId()
                if (existing.userId != currentUserId && !call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. You can only edit your own comments."))
                    return@put
                }

                val request = call.receive<UpdateCommentRequest>()
                try {
                    reviewService.updateComment(id, request)
                    call.respond(HttpStatusCode.OK, ApiResponse("Comment updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // DELETE — свой комментарий или ADMIN/MODERATOR
            delete {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid comment ID"))
                    return@delete
                }

                val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
                val existing = reviewService.getCommentById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Comment not found"))
                    return@delete
                }

                val currentUserId = call.getUserId()
                if (existing.userId != currentUserId && !call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. You can only delete your own comments."))
                    return@delete
                }

                try {
                    reviewService.deleteComment(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("Comment deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }
    }

    // ============================================================
    // ОТЗЫВЫ ПОЛЬЗОВАТЕЛЯ — /api/users/{userId}/reviews
    // ============================================================

    route("/api/users/{userId}/reviews") {
        get {
            val userId = call.parameters["userId"]?.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                return@get
            }

            val reviewService = call.application.attributes[DatabaseConfig.ReviewServiceKey]
            val reviews = reviewService.getReviewsByUser(userId)
            call.respond(HttpStatusCode.OK, reviews)
        }
    }
}