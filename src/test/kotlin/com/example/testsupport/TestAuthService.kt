package com.example.testsupport

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.Token
import com.example.models.User
import com.example.repositories.TokenRepository
import com.example.repositories.UserRepository
import com.example.services.AuthService
import com.example.services.UserService
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

/**
 * Тестовая версия AuthService — генерирует JWT с TEST_JWT_SECRET,
 * чтобы не зависеть от .env файла.
 */
class TestAuthService(
    userRepository: UserRepository,
    tokenRepository: TokenRepository
) : AuthService(userRepository, tokenRepository) {

    private val algorithm = Algorithm.HMAC256(TEST_JWT_SECRET)

    override suspend fun login(email: String, password: String): Pair<User, Token>? {
        val user = userRepository.findByEmail(email) ?: return null

        if (user.isBanned) throw Exception("User is banned")
        if (!UserService.verifyPassword(password, user.passwordHash)) return null

        val accessToken  = generateTestAccessToken(user.id!!, user.email, user.role.name)
        val refreshToken = generateTestRefreshToken(user.id)
        val now = LocalDateTime.now()

        val token = Token(
            userId           = user.id,
            token            = accessToken,
            refreshToken     = refreshToken,
            expiresAt        = now.plusHours(1),
            refreshExpiresAt = now.plusDays(30)
        )
        tokenRepository.create(token)
        return Pair(user, token)
    }

    override suspend fun refreshToken(refreshToken: String): Token? {
        val oldToken = tokenRepository.findByRefreshToken(refreshToken) ?: return null

        val decoded = try {
            JWT.require(algorithm).withIssuer(TEST_JWT_ISSUER).build().verify(oldToken.refreshToken)
        } catch (e: Exception) { return null }

        val userId = decoded.getClaim("userId").asInt()
        val user   = userRepository.findById(userId) ?: return null

        if (user.isBanned) throw Exception("User is banned")

        tokenRepository.revokeToken(oldToken.token)

        val newAccess  = generateTestAccessToken(user.id!!, user.email, user.role.name)
        val newRefresh = generateTestRefreshToken(user.id)
        val now = LocalDateTime.now()

        val newToken = Token(
            userId           = user.id,
            token            = newAccess,
            refreshToken     = newRefresh,
            expiresAt        = now.plusHours(1),
            refreshExpiresAt = now.plusDays(30)
        )
        tokenRepository.create(newToken)
        return newToken
    }

    override suspend fun validateToken(token: String): User? {
        val tokenData = tokenRepository.findByToken(token) ?: return null

        val decoded = try {
            JWT.require(algorithm).withIssuer(TEST_JWT_ISSUER).build().verify(token)
        } catch (e: Exception) {
            return null
        }

        val userId = decoded.getClaim("userId").asInt()
        val user = userRepository.findById(userId) ?: return null
        if (user.isBanned) return null

        return if (!tokenData.isRevoked) user else null
    }

    private fun generateTestAccessToken(userId: Int, email: String, role: String): String {
        val expiresAt = Date.from(
            LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()
        )
        return JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withSubject("Authentication")
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withExpiresAt(expiresAt)
            .withIssuedAt(Date())
            .sign(algorithm)
    }

    private fun generateTestRefreshToken(userId: Int): String {
        val expiresAt = Date.from(
            LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant()
        )
        return JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withSubject("Refresh")
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("userId", userId)
            .withExpiresAt(expiresAt)
            .withIssuedAt(Date())
            .sign(algorithm)
    }
}