package com.example

import com.example.DatabaseFactory.UserServiceKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val message: String,
    val id: Int? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("ReelTrack API is running!")
        }

        get("/health") {
            call.respondText("{\"status\":\"UP\",\"database\":\"connected\"}", ContentType.Application.Json)
        }

        route("/api/users") {
            get {
                val userService = call.application.attributes[UserServiceKey]
                val users = userService.getAllUsers()
                call.respond(users)
            }

            get("/{id}") {
                val userService = call.application.attributes[UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@get
                }

                val user = userService.read(id)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                }
            }

            post {
                val userService = call.application.attributes[UserServiceKey]
                val user = call.receive<User>()
                
                try {
                    val userId = userService.create(user)
                    call.respond(HttpStatusCode.Created, ApiResponse("User created successfully", userId))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            put("/{id}") {
                val userService = call.application.attributes[UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@put
                }

                val user = call.receive<User>()
                
                try {
                    userService.update(id, user)
                    call.respond(HttpStatusCode.OK, ApiResponse("User updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            delete("/{id}") {
                val userService = call.application.attributes[UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                    return@delete
                }

                try {
                    userService.delete(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("User deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            post("/{id}/ban") {
                val userService = call.application.attributes[UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
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
                val userService = call.application.attributes[UserServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()
                
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
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
