package com.example.repositories

import com.example.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class ReviewRepository(private val connection: Connection) {

    fun createTableIfNotExists() {
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS reviews (
                id         SERIAL PRIMARY KEY,
                user_id    INTEGER  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                content_id INTEGER  NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                rating     SMALLINT CHECK (rating BETWEEN 1 AND 10),
                body       TEXT,
                is_spoiler BOOLEAN  NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (user_id, content_id)
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS review_likes (
                user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                review_id  INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, review_id)
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS review_comments (
                id         SERIAL PRIMARY KEY,
                review_id  INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                parent_id  INTEGER REFERENCES review_comments(id) ON DELETE CASCADE,
                body       TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
    }

    // ============================================================
    // REVIEWS
    // ============================================================

    suspend fun create(review: Review): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            INSERT INTO reviews (user_id, content_id, rating, body, is_spoiler)
            VALUES (?, ?, ?, ?, ?) RETURNING id
            """.trimIndent()
        )
        stmt.setInt(1, review.userId)
        stmt.setInt(2, review.contentId)
        stmt.setObject(3, review.rating)
        stmt.setString(4, review.body)
        stmt.setBoolean(5, review.isSpoiler)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt("id") else throw Exception("Failed to create review")
    }

    suspend fun findById(id: Int): ReviewResponse? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT r.*, u.username FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.id = ?"
        )
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toReviewResponse() else null
    }

    suspend fun findByContentId(contentId: Int): List<ReviewResponse> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT r.*, u.username FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.content_id = ? ORDER BY r.created_at DESC"
        )
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<ReviewResponse>()
        while (rs.next()) result += rs.toReviewResponse()
        result
    }

    suspend fun findByUserId(userId: Int): List<ReviewResponse> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT r.*, u.username FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.user_id = ? ORDER BY r.created_at DESC"
        )
        stmt.setInt(1, userId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<ReviewResponse>()
        while (rs.next()) result += rs.toReviewResponse()
        result
    }

    suspend fun update(id: Int, review: Review) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            UPDATE reviews SET rating = ?, body = ?, is_spoiler = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """.trimIndent()
        )
        stmt.setObject(1, review.rating)
        stmt.setString(2, review.body)
        stmt.setBoolean(3, review.isSpoiler)
        stmt.setInt(4, id)
        stmt.executeUpdate()
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM reviews WHERE id = ?")
            .also { it.setInt(1, id) }.executeUpdate()
    }

    // ============================================================
    // LIKES (toggle)
    // ============================================================

    suspend fun toggleLike(userId: Int, reviewId: Int): Boolean = withContext(Dispatchers.IO) {
        val check = connection.prepareStatement(
            "SELECT 1 FROM review_likes WHERE user_id = ? AND review_id = ?"
        )
        check.setInt(1, userId)
        check.setInt(2, reviewId)
        val exists = check.executeQuery().next()

        if (exists) {
            val del = connection.prepareStatement(
                "DELETE FROM review_likes WHERE user_id = ? AND review_id = ?"
            )
            del.setInt(1, userId)
            del.setInt(2, reviewId)
            del.executeUpdate()
            false // убрал лайк
        } else {
            val ins = connection.prepareStatement(
                "INSERT INTO review_likes (user_id, review_id) VALUES (?, ?)"
            )
            ins.setInt(1, userId)
            ins.setInt(2, reviewId)
            ins.executeUpdate()
            true // поставил лайк
        }
    }

    suspend fun getLikesCount(reviewId: Int): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT COUNT(*) FROM review_likes WHERE review_id = ?"
        )
        stmt.setInt(1, reviewId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt(1) else 0
    }

    fun findLikesByReviewId(reviewId: Int): List<ReviewLike> {
        val stmt = connection.prepareStatement(
            """
            SELECT rl.user_id, u.username, rl.created_at
            FROM review_likes rl JOIN users u ON rl.user_id = u.id
            WHERE rl.review_id = ? ORDER BY rl.created_at ASC
            """.trimIndent()
        )
        stmt.setInt(1, reviewId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<ReviewLike>()
        while (rs.next()) result += ReviewLike(
            userId    = rs.getInt("user_id"),
            username  = rs.getString("username"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime()
        )
        return result
    }

    // ============================================================
    // COMMENTS
    // ============================================================

    suspend fun createComment(comment: ReviewComment): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            INSERT INTO review_comments (review_id, user_id, parent_id, body)
            VALUES (?, ?, ?, ?) RETURNING id
            """.trimIndent()
        )
        stmt.setInt(1, comment.reviewId)
        stmt.setInt(2, comment.userId)
        stmt.setObject(3, comment.parentId)
        stmt.setString(4, comment.body)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt("id") else throw Exception("Failed to create comment")
    }

    suspend fun findCommentById(id: Int): ReviewCommentResponse? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            SELECT rc.*, u.username FROM review_comments rc
            JOIN users u ON rc.user_id = u.id
            WHERE rc.id = ?
            """.trimIndent()
        )
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toCommentResponse() else null
    }

    suspend fun findCommentsByReviewId(reviewId: Int): List<ReviewCommentResponse> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            SELECT rc.*, u.username FROM review_comments rc
            JOIN users u ON rc.user_id = u.id
            WHERE rc.review_id = ?
            ORDER BY rc.created_at ASC
            """.trimIndent()
        )
        stmt.setInt(1, reviewId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<ReviewCommentResponse>()
        while (rs.next()) result += rs.toCommentResponse()
        result
    }

    suspend fun updateComment(id: Int, body: String) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "UPDATE review_comments SET body = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
        )
        stmt.setString(1, body)
        stmt.setInt(2, id)
        stmt.executeUpdate()
    }

    suspend fun deleteComment(id: Int) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM review_comments WHERE id = ?")
            .also { it.setInt(1, id) }.executeUpdate()
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun java.sql.ResultSet.toReviewResponse(): ReviewResponse {
        val reviewId = getInt("id")
        val likes    = findLikesByReviewId(reviewId)
        val comments = findCommentsByReviewIdSync(reviewId)
        return ReviewResponse(
            id         = reviewId,
            userId     = getInt("user_id"),
            username   = getString("username"),
            contentId  = getInt("content_id"),
            rating     = getObject("rating")?.let { (it as Number).toShort() },
            body       = getString("body"),
            isSpoiler  = getBoolean("is_spoiler"),
            likesCount = likes.size,
            likes      = likes,
            comments   = comments,
            createdAt  = getTimestamp("created_at").toLocalDateTime(),
            updatedAt  = getTimestamp("updated_at").toLocalDateTime()
        )
    }

    // Синхронная версия для вызова внутри ResultSet-итерации
    private fun findCommentsByReviewIdSync(reviewId: Int): List<ReviewCommentResponse> {
        val stmt = connection.prepareStatement(
            """
            SELECT rc.*, u.username FROM review_comments rc
            JOIN users u ON rc.user_id = u.id
            WHERE rc.review_id = ? ORDER BY rc.created_at ASC
            """.trimIndent()
        )
        stmt.setInt(1, reviewId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<ReviewCommentResponse>()
        while (rs.next()) result += rs.toCommentResponse()
        return result
    }

    private fun java.sql.ResultSet.toCommentResponse() = ReviewCommentResponse(
        id        = getInt("id"),
        reviewId  = getInt("review_id"),
        userId    = getInt("user_id"),
        username  = getString("username"),
        parentId  = getObject("parent_id") as? Int,
        body      = getString("body"),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
        updatedAt = getTimestamp("updated_at").toLocalDateTime()
    )
}