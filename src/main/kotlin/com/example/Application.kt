package com.example

import com.example.config.DatabaseConfig.initializeDatabase
import com.example.config.configureSerialization
import com.example.config.configureRouting
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    initializeDatabase()
    configureRouting()
}
