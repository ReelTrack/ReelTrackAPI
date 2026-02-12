package com.example.config

import com.example.controllers.userRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
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
    }
}
