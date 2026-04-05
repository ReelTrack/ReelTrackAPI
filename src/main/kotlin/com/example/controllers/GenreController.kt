package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.models.*
import com.example.plugins.isAdmin
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.genreRoutes() {
    route("/api/genres") {

        // GET /api/genres — публичный
        get {
            val genreService = call.application.attributes[DatabaseConfig.GenreServiceKey]
            val genres = genreService.getAllGenres()
            call.respond(HttpStatusCode.OK, genres)
        }

        // GET /api/genres/{id} — публичный
        get("/{id}") {
            val genreService = call.application.attributes[DatabaseConfig.GenreServiceKey]
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid genre ID"))
                return@get
            }

            val genre = genreService.getGenreById(id)
            if (genre != null) {
                call.respond(HttpStatusCode.OK, genre)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Genre not found"))
            }
        }

        authenticate("auth-jwt") {

            // POST /api/genres — только ADMIN
            post {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@post
                }

                val genreService = call.application.attributes[DatabaseConfig.GenreServiceKey]
                val request = call.receive<CreateGenreRequest>()

                try {
                    val genre = Genre(name = request.name)
                    val id = genreService.createGenre(genre)
                    call.respond(HttpStatusCode.Created, ApiResponse("Genre created successfully", id))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // PUT /api/genres/{id} — только ADMIN
            put("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@put
                }

                val genreService = call.application.attributes[DatabaseConfig.GenreServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid genre ID"))
                    return@put
                }

                val existing = genreService.getGenreById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Genre not found"))
                    return@put
                }

                val request = call.receive<UpdateGenreRequest>()

                try {
                    genreService.updateGenre(id, Genre(name = request.name))
                    call.respond(HttpStatusCode.OK, ApiResponse("Genre updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // DELETE /api/genres/{id} — только ADMIN
            delete("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@delete
                }

                val genreService = call.application.attributes[DatabaseConfig.GenreServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid genre ID"))
                    return@delete
                }

                val existing = genreService.getGenreById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Genre not found"))
                    return@delete
                }

                try {
                    genreService.deleteGenre(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("Genre deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }
    }
}