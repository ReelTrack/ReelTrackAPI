package com.example.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.github.cdimascio.dotenv.dotenv
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

object JwtConfig {

    private val env = dotenv {
        directory = "./"
        filename = ".env"
        ignoreIfMissing = false
    }

    private val secret = env["JWT_SECRET"]
        ?: throw IllegalStateException("JWT_SECRET is not configured")

    private val issuer = env["JWT_ISSUER"] ?: "ReelTrackAPI"

    val accessTokenExpirationMinutes = 60L // 1 час
    val refreshTokenExpirationDays = 30L // 30 дней

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateAccessToken(userId: Int, email: String, role: String): String {
        val expiresAt = Date.from(
            LocalDateTime.now()
                .plusMinutes(accessTokenExpirationMinutes)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )

        return JWT.create()
            .withIssuer(issuer)
            .withSubject("Authentication")
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withExpiresAt(expiresAt)
            .withIssuedAt(Date())
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: Int): String {
        val expiresAt = Date.from(
            LocalDateTime.now()
                .plusDays(refreshTokenExpirationDays)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )

        return JWT.create()
            .withIssuer(issuer)
            .withSubject("Refresh")
            .withClaim("userId", userId)
            .withExpiresAt(expiresAt)
            .withIssuedAt(Date())
            .sign(algorithm)
    }

    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }

    fun getAccessTokenExpiration(): LocalDateTime {
        return LocalDateTime.now().plusMinutes(accessTokenExpirationMinutes)
    }

    fun getRefreshTokenExpiration(): LocalDateTime {
        return LocalDateTime.now().plusDays(refreshTokenExpirationDays)
    }
}