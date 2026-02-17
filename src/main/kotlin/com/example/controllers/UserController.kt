package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.models.*
import com.example.plugins.getUserId
import com.example.plugins.isAdmin
import com.example.plugins.isModeratorOrAdmin
import com.example.services.UserService
import com.example.utils.toResponse
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/api/users") {

        authenticate("auth-jwt") {

            // Получить всех пользователей - доступно только админам и модераторам
            get {
                if (!call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin or Moderator role required."))
                    return@get
                }

                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val users = userService.getAllUsers()
                val userResponses = users.map { it.toResponse() }
                call.respond(userResponses)
            }

            // Получить пользователя по ID
            get("/{id}") {
                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                val currentUserId = call.getUserId()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@get
                }

                // Пользователь может просматривать только свой профиль, админы и модераторы - любой
                if (id != currentUserId && !call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. You can only view your own profile."))
                    return@get
                }

                val user = userService.getUserById(id)
                if (user != null) {
                    call.respond(user.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                }
            }

            // Создать пользователя - только админы
            post {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@post
                }

                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val request = call.receive<CreateUserRequest>()

                try {
                    val passwordValue = request.getPasswordValue()
                    val hashedPassword = UserService.hashPassword(passwordValue)
                    val user = User(
                        username = request.username,
                        email = request.email,
                        passwordHash = hashedPassword,
                        role = request.role
                    )
                    val userId = userService.createUser(user)
                    call.respond(HttpStatusCode.Created, ApiResponse("User created successfully", userId))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // Обновить пользователя
            put("/{id}") {
                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                val currentUserId = call.getUserId()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@put
                }

                // Пользователь может обновлять только свой профиль, админы - любой
                if (id != currentUserId && !call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. You can only update your own profile."))
                    return@put
                }

                val existingUser = userService.getUserById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                    return@put
                }

                val request = call.receive<UpdateUserRequest>()

                try {
                    // Обычные пользователи не могут изменять роль и статус бана
                    val finalRole = if (call.isAdmin()) request.role else existingUser.role
                    val finalBanStatus = if (call.isAdmin()) request.isBanned else existingUser.isBanned

                    val user = User(
                        username = request.username,
                        email = request.email,
                        passwordHash = "",
                        role = finalRole,
                        isBanned = finalBanStatus
                    )
                    userService.updateUser(id, user, request.getPasswordValue())
                    call.respond(HttpStatusCode.OK, ApiResponse("User updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // Удалить пользователя - только админы
            delete("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@delete
                }

                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@delete
                }

                val existingUser = userService.getUserById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                    return@delete
                }

                try {
                    userService.deleteUser(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("User deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // Забанить пользователя - админы и модераторы
            post("/{id}/ban") {
                if (!call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin or Moderator role required."))
                    return@post
                }

                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@post
                }

                val existingUser = userService.getUserById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                    return@post
                }

                try {
                    userService.banUser(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("User banned successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // Разбанить пользователя - админы и модераторы
            post("/{id}/unban") {
                if (!call.isModeratorOrAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin or Moderator role required."))
                    return@post
                }

                val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@post
                }

                val existingUser = userService.getUserById(id)
                if (existingUser == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                    return@post
                }

                try {
                    userService.unbanUser(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("User unbanned successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }
    }
}