package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.models.*
import com.example.plugins.isAdmin
import com.example.plugins.isModeratorOrAdmin
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.models.ContentType

fun Route.contentRoutes() {
    route("/api/content") {

        // GET /api/content?type=MOVIE&search=...&genreId=1 — публичный
        get {
            val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
            val typeParam = call.request.queryParameters["type"]
            val search = call.request.queryParameters["search"]
            val genreId = call.request.queryParameters["genreId"]?.toIntOrNull()

            val type = typeParam?.let {
                runCatching { ContentType.valueOf(it.uppercase()) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid type. Use MOVIE or SERIES"))
                    return@get
                }
            }

            val items = contentService.getAllContent(type, search, genreId)
            call.respond(HttpStatusCode.OK, items)
        }

        // GET /api/content/{id} — публичный, полный объект
        get("/{id}") {
            val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                return@get
            }

            val content = contentService.getContentById(id)
            if (content != null) {
                call.respond(HttpStatusCode.OK, content)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Content not found"))
            }
        }

        authenticate("auth-jwt") {

            // POST /api/content — только ADMIN
            post {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@post
                }

                val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                val request = call.receive<CreateContentRequest>()

                try {
                    val id = contentService.createContent(request)
                    call.respond(HttpStatusCode.Created, ApiResponse("Content created successfully", id))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // PUT /api/content/{id} — только ADMIN
            put("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@put
                }

                val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                    return@put
                }

                if (contentService.getContentById(id) == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Content not found"))
                    return@put
                }

                val request = call.receive<UpdateContentRequest>()
                try {
                    contentService.updateContent(id, request)
                    call.respond(HttpStatusCode.OK, ApiResponse("Content updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // DELETE /api/content/{id} — только ADMIN
            delete("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@delete
                }

                val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                    return@delete
                }

                if (contentService.getContentById(id) == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Content not found"))
                    return@delete
                }

                try {
                    contentService.deleteContent(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("Content deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }

        // ============================================================
        // SEASONS — /api/content/{contentId}/seasons
        // ============================================================

        route("/{contentId}/seasons") {

            // GET /api/content/{contentId}/seasons — публичный, список сезонов
            get {
                val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                val contentId = call.parameters["contentId"]?.toIntOrNull()

                if (contentId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                    return@get
                }

                val content = contentService.getContentById(contentId)
                if (content == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Content not found"))
                    return@get
                }
                if (content.type != ContentType.SERIES) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Seasons are only available for SERIES"))
                    return@get
                }

                call.respond(HttpStatusCode.OK, content.seasons)
            }

            // GET /api/content/{contentId}/seasons/{seasonId} — публичный, один сезон
            get("/{seasonId}") {
                val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                val seasonId = call.parameters["seasonId"]?.toIntOrNull()

                if (seasonId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid season ID"))
                    return@get
                }

                val season = contentService.getSeasonById(seasonId)
                if (season != null) {
                    call.respond(HttpStatusCode.OK, season)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Season not found"))
                }
            }

            authenticate("auth-jwt") {

                // POST — только ADMIN
                post {
                    if (!call.isAdmin()) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                        return@post
                    }

                    val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                    val contentId = call.parameters["contentId"]?.toIntOrNull()

                    if (contentId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid content ID"))
                        return@post
                    }

                    val content = contentService.getContentById(contentId)
                    if (content == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Content not found"))
                        return@post
                    }
                    if (content.type != ContentType.SERIES) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Seasons can only be added to SERIES"))
                        return@post
                    }

                    val request = call.receive<CreateSeasonRequest>()
                    try {
                        val id = contentService.createSeason(contentId, request)
                        call.respond(HttpStatusCode.Created, ApiResponse("Season created successfully", id))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                    }
                }

                // PUT /{seasonId}
                put("/{seasonId}") {
                    if (!call.isAdmin()) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                        return@put
                    }

                    val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                    val seasonId = call.parameters["seasonId"]?.toIntOrNull()

                    if (seasonId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid season ID"))
                        return@put
                    }

                    if (contentService.getSeasonById(seasonId) == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Season not found"))
                        return@put
                    }

                    val request = call.receive<UpdateSeasonRequest>()
                    try {
                        contentService.updateSeason(seasonId, request)
                        call.respond(HttpStatusCode.OK, ApiResponse("Season updated successfully"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                    }
                }

                // DELETE /{seasonId}
                delete("/{seasonId}") {
                    if (!call.isAdmin()) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                        return@delete
                    }

                    val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                    val seasonId = call.parameters["seasonId"]?.toIntOrNull()

                    if (seasonId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid season ID"))
                        return@delete
                    }

                    if (contentService.getSeasonById(seasonId) == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Season not found"))
                        return@delete
                    }

                    try {
                        contentService.deleteSeason(seasonId)
                        call.respond(HttpStatusCode.OK, ApiResponse("Season deleted successfully"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                    }
                }

                // ============================================================
                // EPISODES — /api/content/{contentId}/seasons/{seasonId}/episodes
                // ============================================================

                route("/{seasonId}/episodes") {

                    // POST
                    post {
                        if (!call.isAdmin()) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                            return@post
                        }

                        val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                        val seasonId = call.parameters["seasonId"]?.toIntOrNull()

                        if (seasonId == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid season ID"))
                            return@post
                        }

                        if (contentService.getSeasonById(seasonId) == null) {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Season not found"))
                            return@post
                        }

                        val request = call.receive<CreateEpisodeRequest>()
                        try {
                            val id = contentService.createEpisode(seasonId, request)
                            call.respond(HttpStatusCode.Created, ApiResponse("Episode created successfully", id))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                        }
                    }

                    // PUT /{episodeId}
                    put("/{episodeId}") {
                        if (!call.isAdmin()) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                            return@put
                        }

                        val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                        val episodeId = call.parameters["episodeId"]?.toIntOrNull()

                        if (episodeId == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid episode ID"))
                            return@put
                        }

                        if (contentService.getEpisodeById(episodeId) == null) {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Episode not found"))
                            return@put
                        }

                        val request = call.receive<UpdateEpisodeRequest>()
                        try {
                            contentService.updateEpisode(episodeId, request)
                            call.respond(HttpStatusCode.OK, ApiResponse("Episode updated successfully"))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                        }
                    }

                    // DELETE /{episodeId}
                    delete("/{episodeId}") {
                        if (!call.isAdmin()) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                            return@delete
                        }

                        val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                        val episodeId = call.parameters["episodeId"]?.toIntOrNull()

                        if (episodeId == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid episode ID"))
                            return@delete
                        }

                        if (contentService.getEpisodeById(episodeId) == null) {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Episode not found"))
                            return@delete
                        }

                        try {
                            contentService.deleteEpisode(episodeId)
                            call.respond(HttpStatusCode.OK, ApiResponse("Episode deleted successfully"))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                        }
                    }
                }
            }

            // GET /api/content/{contentId}/seasons/{seasonId}/episodes/{episodeId} — публичный
            get("/{seasonId}/episodes/{episodeId}") {
                val contentService = call.application.attributes[DatabaseConfig.ContentServiceKey]
                val episodeId = call.parameters["episodeId"]?.toIntOrNull()

                if (episodeId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid episode ID"))
                    return@get
                }

                val episode = contentService.getEpisodeById(episodeId)
                if (episode != null) {
                    call.respond(HttpStatusCode.OK, episode)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Episode not found"))
                }
            }
        }
    }
}