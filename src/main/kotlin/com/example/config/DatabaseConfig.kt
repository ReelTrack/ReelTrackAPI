package com.example.config

import com.example.repositories.ContentRepository
import com.example.repositories.GenreRepository
import com.example.repositories.PersonRepository
import com.example.repositories.ReviewRepository
import com.example.repositories.TokenRepository
import com.example.repositories.UserRepository
import com.example.services.AuthService
import com.example.services.ContentService
import com.example.services.GenreService
import com.example.services.PersonService
import com.example.services.ReviewService
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
        ignoreIfMissing = false
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

            // Repositories
            val userRepository    = UserRepository(connection)
            val tokenRepository   = TokenRepository(connection)
            val genreRepository   = GenreRepository(connection)
            val personRepository  = PersonRepository(connection)
            val contentRepository = ContentRepository(connection)
            // ReviewRepository получает ссылку на contentRepository для пересчёта avg_rating
            val reviewRepository  = ReviewRepository(connection, contentRepository)

            // Init tables (порядок важен — reviews зависит от users и content)
            userRepository.createTableIfNotExists()
            tokenRepository.createTableIfNotExists()
            genreRepository.createTableIfNotExists()
            personRepository.createTableIfNotExists()
            contentRepository.createTableIfNotExists()
            reviewRepository.createTableIfNotExists()

            // Services
            val userService    = UserService(userRepository)
            val authService    = AuthService(userRepository, tokenRepository)
            val genreService   = GenreService(genreRepository)
            val personService  = PersonService(personRepository)
            val contentService = ContentService(contentRepository)
            val reviewService  = ReviewService(reviewRepository)

            // Register in attributes
            attributes.put(ConnectionKey,     connection)
            attributes.put(UserServiceKey,    userService)
            attributes.put(AuthServiceKey,    authService)
            attributes.put(GenreServiceKey,   genreService)
            attributes.put(PersonServiceKey,  personService)
            attributes.put(ContentServiceKey, contentService)
            attributes.put(ReviewServiceKey,  reviewService)

            environment.log.info("✅ All database tables initialized successfully!")
        } catch (e: Exception) {
            environment.log.error("❌ Failed to connect to database", e)
            throw e
        }
    }

    val ConnectionKey     = AttributeKey<Connection>("db.connection")
    val UserServiceKey    = AttributeKey<UserService>("user.service")
    val AuthServiceKey    = AttributeKey<AuthService>("auth.service")
    val GenreServiceKey   = AttributeKey<GenreService>("genre.service")
    val PersonServiceKey  = AttributeKey<PersonService>("person.service")
    val ContentServiceKey = AttributeKey<ContentService>("content.service")
    val ReviewServiceKey  = AttributeKey<ReviewService>("review.service")
}