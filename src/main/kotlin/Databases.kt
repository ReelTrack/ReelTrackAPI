package com.example

import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import java.sql.Connection
import java.sql.DriverManager

object DatabaseFactory {
    private val dbUrl = System.getenv("DB_POSTGRES_URL") 
        ?: System.getProperty("DB_POSTGRES_URL")
        ?: "jdbc:postgresql://localhost:5432/ReelTrackDB".also {
            println("⚠️  WARNING: Using default database URL. Set DB_POSTGRES_URL environment variable for production!")
        }
    
    private val dbUser = System.getenv("DB_POSTGRES_USER") 
        ?: System.getProperty("DB_POSTGRES_USER")
        ?: "postgres".also {
            println("⚠️  WARNING: Using default database user. Set DB_POSTGRES_USER environment variable for production!")
        }
    
    private val dbPassword = System.getenv("DB_POSTGRES_PASSWORD") 
        ?: System.getProperty("DB_POSTGRES_PASSWORD")
        ?: "271104Nik@".also {
            println("⚠️  WARNING: Using default database password. Set DB_POSTGRES_PASSWORD environment variable for production!")
        }

    fun Application.initializationDatabase() {
        try {
            Class.forName("org.postgresql.Driver")
            val connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
            environment.log.info("Database connected successfully to ReelTrackDB!")

            val userService = UserSchema(connection)
            userService.createTableIfNotExists()

            attributes.put(ConnectionKey, connection)
            attributes.put(UserServiceKey, userService)
        } catch (e: Exception) {
            environment.log.error("Failed to connect to database: ${e.message}", e)
            throw e
        }
    }

    val ConnectionKey = AttributeKey<Connection>("db.connection")
    val UserServiceKey = AttributeKey<UserSchema>("user.service")
}