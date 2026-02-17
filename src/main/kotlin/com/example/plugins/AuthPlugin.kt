package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.ErrorResponse
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {
    val env = dotenv {
        directory = "./"
        filename = ".env"
        ignoreIfMissing = false
    }

    val secret = env["JWT_SECRET"]
        ?: throw IllegalStateException("JWT_SECRET is not configured")

    val issuer = env["JWT_ISSUER"] ?: "ReelTrackAPI"

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                val email = credential.payload.getClaim("email").asString()
                val role = credential.payload.getClaim("role").asString()

                if (userId != null && email != null && role != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token is not valid or has expired"))
            }
        }
    }
}

// Extension для получения userId из токена
fun ApplicationCall.getUserId(): Int {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("userId")?.asInt()
        ?: throw IllegalStateException("User ID not found in token")
}

// Extension для получения email из токена
fun ApplicationCall.getUserEmail(): String {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("email")?.asString()
        ?: throw IllegalStateException("Email not found in token")
}

// Extension для получения роли из токена
fun ApplicationCall.getUserRole(): String {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("role")?.asString()
        ?: throw IllegalStateException("Role not found in token")
}

// Extension для проверки роли администратора
fun ApplicationCall.isAdmin(): Boolean {
    return try {
        getUserRole() == "ADMIN"
    } catch (e: Exception) {
        false
    }
}

// Extension для проверки роли модератора или администратора
fun ApplicationCall.isModeratorOrAdmin(): Boolean {
    return try {
        val role = getUserRole()
        role == "ADMIN" || role == "MODERATOR"
    } catch (e: Exception) {
        false
    }
}