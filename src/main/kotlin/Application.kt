package com.example

import com.example.DatabaseFactory.initializationDatabase
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    initializationDatabase()
    configureRouting()
}
