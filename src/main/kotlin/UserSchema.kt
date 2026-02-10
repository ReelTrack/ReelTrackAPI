package com.example

import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.sql.Connection
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

@Serializable
enum class UserRole {
    USER, ADMIN, MODERATOR
}

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

class UserSchema(private val connection: Connection) {
    companion object {
        private const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(255) NOT NULL UNIQUE,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL DEFAULT 'USER',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_banned BOOLEAN NOT NULL DEFAULT FALSE
            )
            """
        private const val INSERT_USER =
            "INSERT INTO users (username, email, password_hash, role, is_banned, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        private const val SELECT_USER_BY_ID = "SELECT * FROM users WHERE id = ?"
        private const val SELECT_USER_BY_EMAIL = "SELECT * FROM users WHERE email = ?"
        private const val UPDATE_USER =
            "UPDATE users SET username = ?, email = ?, role = ?, is_banned = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
        private const val DELETE_USER = "DELETE FROM users WHERE id = ?"
        private const val SELECT_ALL_USERS = "SELECT * FROM users"
        private const val BAN_USER = "UPDATE users SET is_banned = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
        private const val UNBAN_USER = "UPDATE users SET is_banned = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
    }

    fun createTableIfNotExists() {
        connection.createStatement().use { statement ->
            statement.execute(CREATE_TABLE)
        }
    }

    suspend fun create(user: User): Int = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, user.username)
        statement.setString(2, user.email)
        statement.setString(3, user.passwordHash)
        statement.setString(4, user.role.name)
        statement.setBoolean(5, user.isBanned)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw Exception("Unable to retrieve the id of the newly inserted user")
        }
    }

    suspend fun read(id: Int): User? = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_USER_BY_ID)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext mapResultSetToUser(resultSet)
        }
        return@withContext null
    }

    suspend fun findByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_USER_BY_EMAIL)
        statement.setString(1, email)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext mapResultSetToUser(resultSet)
        }
        return@withContext null
    }

    suspend fun update(id: Int, user: User) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(UPDATE_USER)
        statement.setString(1, user.username)
        statement.setString(2, user.email)
        statement.setString(3, user.role.name)
        statement.setBoolean(4, user.isBanned)
        statement.setInt(5, id)
        statement.executeUpdate()
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(DELETE_USER)
        statement.setInt(1, id)
        statement.executeUpdate()
    }

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(SELECT_ALL_USERS)

        val users = mutableListOf<User>()
        while (resultSet.next()) {
            users.add(mapResultSetToUser(resultSet))
        }
        return@withContext users
    }

    suspend fun banUser(id: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(BAN_USER)
        statement.setInt(1, id)
        statement.executeUpdate()
    }

    suspend fun unbanUser(id: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(UNBAN_USER)
        statement.setInt(1, id)
        statement.executeUpdate()
    }

    private fun mapResultSetToUser(resultSet: java.sql.ResultSet): User {
        return User(
            id = resultSet.getInt("id"),
            username = resultSet.getString("username"),
            email = resultSet.getString("email"),
            passwordHash = resultSet.getString("password_hash"),
            role = UserRole.valueOf(resultSet.getString("role")),
            createdAt = resultSet.getTimestamp("created_at")?.toLocalDateTime(),
            updatedAt = resultSet.getTimestamp("updated_at")?.toLocalDateTime(),
            isBanned = resultSet.getBoolean("is_banned")
        )
    }
}