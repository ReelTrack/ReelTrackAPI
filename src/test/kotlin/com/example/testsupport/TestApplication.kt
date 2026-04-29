package com.example.testsupport

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.config.*
import com.example.controllers.*
import com.example.models.ErrorResponse
import com.example.repositories.*
import com.example.services.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import java.sql.Connection

internal const val TEST_JWT_SECRET = "test-secret-key-for-unit-tests-only"
internal const val TEST_JWT_ISSUER = "ReelTrackTestAPI"

fun Application.testModule(connection: Connection) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(TEST_JWT_SECRET))
                    .withIssuer(TEST_JWT_ISSUER)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                val email  = credential.payload.getClaim("email").asString()
                val role   = credential.payload.getClaim("role").asString()
                if (userId != null && email != null && role != null)
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token is not valid or has expired"))
            }
        }
    }

    val contentRepo = ContentRepository(connection)
    // ReviewRepository с одним аргументом (без contentRepository)
    val reviewRepo  = ReviewRepository(connection)
    val userRepo    = UserRepository(connection)
    val tokenRepo   = TokenRepository(connection)
    val genreRepo   = GenreRepository(connection)
    val personRepo  = PersonRepository(connection)

    val userService    = UserService(userRepo)
    val authService    = TestAuthService(userRepo, tokenRepo)
    val genreService   = GenreService(genreRepo)
    val personService  = PersonService(personRepo)
    val contentService = ContentService(contentRepo)
    val reviewService  = ReviewService(reviewRepo)

    attributes.put(DatabaseConfig.ConnectionKey,     connection)
    attributes.put(DatabaseConfig.UserServiceKey,    userService)
    attributes.put(DatabaseConfig.AuthServiceKey,    authService)
    attributes.put(DatabaseConfig.GenreServiceKey,   genreService)
    attributes.put(DatabaseConfig.PersonServiceKey,  personService)
    attributes.put(DatabaseConfig.ContentServiceKey, contentService)
    attributes.put(DatabaseConfig.ReviewServiceKey,  reviewService)

    routing {
        userRoutes()
        authRoutes()
        genreRoutes()
        personRoutes()
        contentRoutes()
        reviewRoutes()
    }
}