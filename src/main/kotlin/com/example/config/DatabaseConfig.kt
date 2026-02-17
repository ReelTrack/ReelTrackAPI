package com.example.config

import com.example.repositories.TokenRepository
import com.example.repositories.UserRepository
import com.example.services.AuthService
import com.example.services.UserService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import java.sql.Connection
import java.sql.DriverManager

object DatabaseConfig {

    private val env = dotenv {
        directory = "./"
        filename = ".env"
        ignoreIfMissing = false // если файл не найден - ошибка
    }

    private val dbUrl = env["DB_POSTGRES_URL"]
        ?: throw IllegalStateException("DB_POSTGRES_URL is not configured")

    private val dbUser = env["DB_POSTGRES_USER"]
        ?: throw IllegalStateException("DB_POSTGRES_USER is not configured")

    private val dbPassword = env["DB_POSTGRES_PASSWORD"]
        ?: throw IllegalStateException("DB_POSTGRES_PASSWORD is not configured")

    fun Application.initializeDatabase() {
        try {
            Class.forName("org.postgresql.Driver")
            val connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
            environment.log.info("✅ Database connected successfully!")
            environment.log.info("🗄️  Connected to: ${dbUrl.replaceAfter("@", "***")}")

            val userRepository = UserRepository(connection)
            userRepository.createTableIfNotExists()

            val tokenRepository = TokenRepository(connection)
            tokenRepository.createTableIfNotExists()

            val userService = UserService(userRepository)
            val authService = AuthService(userRepository, tokenRepository)

            attributes.put(ConnectionKey, connection)
            attributes.put(UserServiceKey, userService)
            attributes.put(AuthServiceKey, authService)

            environment.log.info("✅ All database tables initialized successfully!")
        } catch (e: Exception) {
            environment.log.error("❌ Failed to connect to database", e)
            throw e
        }
    }

    val ConnectionKey = AttributeKey<Connection>("db.connection")
    val UserServiceKey = AttributeKey<UserService>("user.service")
    val AuthServiceKey = AttributeKey<AuthService>("auth.service")
}