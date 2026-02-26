package com.example.services

import com.example.config.JwtConfig
import com.example.models.Token
import com.example.models.User
import com.example.repositories.TokenRepository
import com.example.repositories.UserRepository

class AuthService(
    val userRepository: UserRepository,
    val tokenRepository: TokenRepository
) {

    suspend fun login(email: String, password: String): Pair<User, Token>? {
        val user = userRepository.findByEmail(email) ?: return null

        if (user.isBanned) {
            throw Exception("User is banned")
        }

        if (!UserService.verifyPassword(password, user.passwordHash)) {
            return null
        }

        val accessToken = JwtConfig.generateAccessToken(user.id!!, user.email, user.role.name)
        val refreshToken = JwtConfig.generateRefreshToken(user.id)

        val token = Token(
            userId = user.id,
            token = accessToken,
            refreshToken = refreshToken,
            expiresAt = JwtConfig.getAccessTokenExpiration(),
            refreshExpiresAt = JwtConfig.getRefreshTokenExpiration()
        )

        tokenRepository.create(token)

        return Pair(user, token)
    }

    suspend fun refreshToken(refreshToken: String): Token? {
        val oldToken = tokenRepository.findByRefreshToken(refreshToken) ?: return null

        val decoded = JwtConfig.verifyToken(oldToken.refreshToken) ?: return null
        val userId = decoded.getClaim("userId").asInt()

        val user = userRepository.findById(userId) ?: return null

        if (user.isBanned) {
            throw Exception("User is banned")
        }

        tokenRepository.revokeToken(oldToken.token)

        val newAccessToken = JwtConfig.generateAccessToken(user.id!!, user.email, user.role.name)
        val newRefreshToken = JwtConfig.generateRefreshToken(user.id)

        val newToken = Token(
            userId = user.id,
            token = newAccessToken,
            refreshToken = newRefreshToken,
            expiresAt = JwtConfig.getAccessTokenExpiration(),
            refreshExpiresAt = JwtConfig.getRefreshTokenExpiration()
        )

        tokenRepository.create(newToken)

        return newToken
    }

    suspend fun logout(token: String) {
        tokenRepository.revokeToken(token)
    }

    suspend fun logoutAllDevices(userId: Int) {
        tokenRepository.revokeAllUserTokens(userId)
    }

    suspend fun validateToken(token: String): User? {
        val tokenData = tokenRepository.findByToken(token) ?: return null

        val decoded = JwtConfig.verifyToken(token) ?: return null
        val userId = decoded.getClaim("userId").asInt()

        val user = userRepository.findById(userId) ?: return null

        if (user.isBanned) {
            return null
        }

        return user
    }
}