package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.config.JwtConfig
import com.example.models.*
import com.example.services.AuthService
import com.example.utils.toResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val authService = call.application.attributes[DatabaseConfig.AuthServiceKey]
            val request = call.receive<CreateUserRequest>()

            try {
                val existingUser = authService.userRepository.findByEmail(request.email)
                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("User with this email already exists"))
                    return@post
                }

                val passwordValue = request.getPasswordValue()
                val hashedPassword = com.example.services.UserService.hashPassword(passwordValue)
                val user = User(
                    username = request.username,
                    email = request.email,
                    passwordHash = hashedPassword,
                    role = request.role
                )

                val userId = authService.userRepository.create(user)

                call.respond(HttpStatusCode.Created, ApiResponse("User registered successfully", userId))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        post("/login") {
            val authService = call.application.attributes[DatabaseConfig.AuthServiceKey]
            val request = call.receive<LoginRequest>()

            try {
                val result = authService.login(request.email, request.password)

                if (result == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid email or password"))
                    return@post
                }

                val (user, token) = result
                val response = LoginResponse(
                    accessToken = token.token,
                    refreshToken = token.refreshToken,
                    expiresIn = JwtConfig.accessTokenExpirationMinutes * 60,
                    user = user.toResponse()
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                if (e.message?.contains("banned") == true) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("User is banned"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }

        post("/refresh") {
            val authService = call.application.attributes[DatabaseConfig.AuthServiceKey]
            val request = call.receive<RefreshTokenRequest>()

            try {
                val newToken = authService.refreshToken(request.refreshToken)

                if (newToken == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired refresh token"))
                    return@post
                }

                val response = TokenResponse(
                    accessToken = newToken.token,
                    refreshToken = newToken.refreshToken,
                    expiresIn = JwtConfig.accessTokenExpirationMinutes * 60
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                if (e.message?.contains("banned") == true) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("User is banned"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(e.message ?: "Invalid refresh token"))
                }
            }
        }

        post("/logout") {
            val authService = call.application.attributes[DatabaseConfig.AuthServiceKey]
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")

            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("No token provided"))
                return@post
            }

            try {
                authService.logout(token)
                call.respond(HttpStatusCode.OK, ApiResponse("Logged out successfully"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        post("/logout-all") {
            val authService = call.application.attributes[DatabaseConfig.AuthServiceKey]
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")

            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("No token provided"))
                return@post
            }

            try {
                val user = authService.validateToken(token)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                    return@post
                }

                authService.logoutAllDevices(user.id!!)
                call.respond(HttpStatusCode.OK, ApiResponse("Logged out from all devices"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        get("/me") {
            val authService = call.application.attributes[DatabaseConfig.AuthServiceKey]
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")

            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("No token provided"))
                return@get
            }

            try {
                val user = authService.validateToken(token)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired token"))
                    return@get
                }

                call.respond(HttpStatusCode.OK, user.toResponse())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(e.message ?: "Invalid token"))
            }
        }
    }
}