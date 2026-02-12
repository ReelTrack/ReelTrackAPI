package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.models.*
import com.example.services.UserService
import com.example.utils.toResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/api/users") {
        get {
            val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
            val users = userService.getAllUsers()
            val userResponses = users.map { it.toResponse() }
            call.respond(userResponses)
        }

        get("/{id}") {
            val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                return@get
            }

            val user = userService.getUserById(id)
            if (user != null) {
                call.respond(user.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            }
        }

        post {
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

        put("/{id}") {
            val userService = call.application.attributes[DatabaseConfig.UserServiceKey]
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                return@put
            }

            val existingUser = userService.getUserById(id)
            if (existingUser == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                return@put
            }

            val request = call.receive<UpdateUserRequest>()
            
            try {
                val user = User(
                    username = request.username,
                    email = request.email,
                    passwordHash = "",
                    role = request.role,
                    isBanned = request.isBanned
                )
                userService.updateUser(id, user, request.getPasswordValue())
                call.respond(HttpStatusCode.OK, ApiResponse("User updated successfully"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        delete("/{id}") {
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

        post("/{id}/ban") {
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

        post("/{id}/unban") {
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
