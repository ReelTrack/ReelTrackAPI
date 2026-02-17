package com.example.repositories

import com.example.models.Token
import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

class TokenRepository(private val connection: Connection) {
    companion object {
        private const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS tokens (
                id SERIAL PRIMARY KEY,
                user_id INTEGER NOT NULL,
                token TEXT NOT NULL UNIQUE,
                refresh_token TEXT NOT NULL UNIQUE,
                expires_at TIMESTAMP NOT NULL,
                refresh_expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """
        private const val CREATE_INDEX_USER_ID = "CREATE INDEX IF NOT EXISTS idx_tokens_user_id ON tokens(user_id)"
        private const val CREATE_INDEX_TOKEN = "CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token)"
        private const val CREATE_INDEX_REFRESH = "CREATE INDEX IF NOT EXISTS idx_tokens_refresh_token ON tokens(refresh_token)"

        private const val INSERT_TOKEN = """
            INSERT INTO tokens (user_id, token, refresh_token, expires_at, refresh_expires_at, created_at, is_revoked) 
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE)
        """
        private const val FIND_BY_TOKEN = "SELECT * FROM tokens WHERE token = ? AND is_revoked = FALSE AND expires_at > CURRENT_TIMESTAMP"
        private const val FIND_BY_REFRESH_TOKEN = "SELECT * FROM tokens WHERE refresh_token = ? AND is_revoked = FALSE AND refresh_expires_at > CURRENT_TIMESTAMP"
        private const val REVOKE_TOKEN = "UPDATE tokens SET is_revoked = TRUE WHERE token = ?"
        private const val REVOKE_ALL_USER_TOKENS = "UPDATE tokens SET is_revoked = TRUE WHERE user_id = ?"
        private const val DELETE_EXPIRED = "DELETE FROM tokens WHERE expires_at < CURRENT_TIMESTAMP OR is_revoked = TRUE"
    }

    fun createTableIfNotExists() {
        connection.createStatement().use { statement ->
            statement.execute(CREATE_TABLE)
            statement.execute(CREATE_INDEX_USER_ID)
            statement.execute(CREATE_INDEX_TOKEN)
            statement.execute(CREATE_INDEX_REFRESH)
        }
    }

    suspend fun create(token: Token): Int = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_TOKEN, Statement.RETURN_GENERATED_KEYS)
        statement.setInt(1, token.userId)
        statement.setString(2, token.token)
        statement.setString(3, token.refreshToken)
        statement.setTimestamp(4, Timestamp.valueOf(token.expiresAt))
        statement.setTimestamp(5, Timestamp.valueOf(token.refreshExpiresAt))
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw Exception("Unable to retrieve the id of the newly inserted token")
        }
    }

    suspend fun findByToken(token: String): Token? = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(FIND_BY_TOKEN)
        statement.setString(1, token)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext mapResultSetToToken(resultSet)
        }
        return@withContext null
    }

    suspend fun findByRefreshToken(refreshToken: String): Token? = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(FIND_BY_REFRESH_TOKEN)
        statement.setString(1, refreshToken)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext mapResultSetToToken(resultSet)
        }
        return@withContext null
    }

    suspend fun revokeToken(token: String) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(REVOKE_TOKEN)
        statement.setString(1, token)
        statement.executeUpdate()
    }

    suspend fun revokeAllUserTokens(userId: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(REVOKE_ALL_USER_TOKENS)
        statement.setInt(1, userId)
        statement.executeUpdate()
    }

    suspend fun deleteExpiredTokens() = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(DELETE_EXPIRED)
        statement.executeUpdate()
    }

    private fun mapResultSetToToken(resultSet: java.sql.ResultSet): Token {
        return Token(
            id = resultSet.getInt("id"),
            userId = resultSet.getInt("user_id"),
            token = resultSet.getString("token"),
            refreshToken = resultSet.getString("refresh_token"),
            expiresAt = resultSet.getTimestamp("expires_at").toLocalDateTime(),
            refreshExpiresAt = resultSet.getTimestamp("refresh_expires_at").toLocalDateTime(),
            createdAt = resultSet.getTimestamp("created_at")?.toLocalDateTime(),
            isRevoked = resultSet.getBoolean("is_revoked")
        )
    }
}