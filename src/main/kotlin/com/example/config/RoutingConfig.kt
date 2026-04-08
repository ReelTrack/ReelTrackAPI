package com.example.config

import com.example.controllers.authRoutes
import com.example.controllers.contentRoutes
import com.example.controllers.genreRoutes
import com.example.controllers.personRoutes
import com.example.controllers.reviewRoutes
import com.example.controllers.userRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")

        get("/") {
            call.respondText("ReelTrack API is running!")
        }

        get("/health") {
            call.respondText(
                """{"status":"UP","database":"connected"}""",
                ContentType.Application.Json
            )
        }

        userRoutes()
        authRoutes()
        genreRoutes()
        personRoutes()
        contentRoutes()
        reviewRoutes()
    }
}