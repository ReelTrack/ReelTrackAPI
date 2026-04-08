package com.example.services

import com.example.models.*
import com.example.repositories.ReviewRepository

class ReviewService(private val reviewRepository: ReviewRepository) {

    // ============================================================
    // REVIEWS
    // ============================================================

    suspend fun createReview(userId: Int, contentId: Int, request: CreateReviewRequest): Int {
        if (request.rating == null && request.body.isNullOrBlank()) {
            throw Exception("Review must have at least a rating or a body")
        }
        request.rating?.let {
            if (it < 1 || it > 10) throw Exception("Rating must be between 1 and 10")
        }
        val review = Review(
            userId    = userId,
            contentId = contentId,
            rating    = request.rating,
            body      = request.body,
            isSpoiler = request.isSpoiler
        )
        return reviewRepository.create(review)
    }

    suspend fun getReviewById(id: Int): ReviewResponse? {
        return reviewRepository.findById(id)
    }

    suspend fun getReviewsByContent(contentId: Int): List<ReviewResponse> {
        return reviewRepository.findByContentId(contentId)
    }

    suspend fun getReviewsByUser(userId: Int): List<ReviewResponse> {
        return reviewRepository.findByUserId(userId)
    }

    suspend fun updateReview(id: Int, request: UpdateReviewRequest) {
        if (request.rating == null && request.body.isNullOrBlank()) {
            throw Exception("Review must have at least a rating or a body")
        }
        request.rating?.let {
            if (it < 1 || it > 10) throw Exception("Rating must be between 1 and 10")
        }
        val review = Review(
            userId    = 0,
            contentId = 0,
            rating    = request.rating,
            body      = request.body,
            isSpoiler = request.isSpoiler
        )
        reviewRepository.update(id, review)
    }

    suspend fun deleteReview(id: Int) {
        reviewRepository.delete(id)
    }

    // ============================================================
    // LIKES
    // ============================================================

    suspend fun toggleLike(userId: Int, reviewId: Int): LikeResponse {
        val liked = reviewRepository.toggleLike(userId, reviewId)
        val count = reviewRepository.getLikesCount(reviewId)
        return LikeResponse(liked = liked, likesCount = count)
    }

    suspend fun getLikesByReview(reviewId: Int): List<ReviewLike> {
        return reviewRepository.findLikesByReviewId(reviewId)
    }

    // ============================================================
    // COMMENTS
    // ============================================================

    suspend fun createComment(userId: Int, reviewId: Int, request: CreateCommentRequest): Int {
        if (request.body.isBlank()) throw Exception("Comment body cannot be empty")
        val comment = ReviewComment(
            reviewId = reviewId,
            userId   = userId,
            parentId = request.parentId,
            body     = request.body
        )
        return reviewRepository.createComment(comment)
    }

    suspend fun getCommentById(id: Int): ReviewCommentResponse? {
        return reviewRepository.findCommentById(id)
    }

    suspend fun getCommentsByReview(reviewId: Int): List<ReviewCommentResponse> {
        return reviewRepository.findCommentsByReviewId(reviewId)
    }

    suspend fun updateComment(id: Int, request: UpdateCommentRequest) {
        if (request.body.isBlank()) throw Exception("Comment body cannot be empty")
        reviewRepository.updateComment(id, request.body)
    }

    suspend fun deleteComment(id: Int) {
        reviewRepository.deleteComment(id)
    }
}