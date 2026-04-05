package com.example.repositories

import com.example.models.Genre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class GenreRepository(private val connection: Connection) {

    fun createTableIfNotExists() {
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS genres (
                id   SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL UNIQUE
            )
            """.trimIndent()
        )
    }

    suspend fun create(genre: Genre): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "INSERT INTO genres (name) VALUES (?) RETURNING id"
        )
        stmt.setString(1, genre.name)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt("id")
        else throw Exception("Failed to create genre")
    }

    suspend fun findById(id: Int): Genre? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT * FROM genres WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) Genre(id = rs.getInt("id"), name = rs.getString("name"))
        else null
    }

    suspend fun findByName(name: String): Genre? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT * FROM genres WHERE LOWER(name) = LOWER(?)")
        stmt.setString(1, name)
        val rs = stmt.executeQuery()
        if (rs.next()) Genre(id = rs.getInt("id"), name = rs.getString("name"))
        else null
    }

    suspend fun findAll(): List<Genre> = withContext(Dispatchers.IO) {
        val rs = connection.createStatement().executeQuery("SELECT * FROM genres ORDER BY name")
        val result = mutableListOf<Genre>()
        while (rs.next()) {
            result += Genre(id = rs.getInt("id"), name = rs.getString("name"))
        }
        result
    }

    suspend fun update(id: Int, genre: Genre) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("UPDATE genres SET name = ? WHERE id = ?")
        stmt.setString(1, genre.name)
        stmt.setInt(2, id)
        stmt.executeUpdate()
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("DELETE FROM genres WHERE id = ?")
        stmt.setInt(1, id)
        stmt.executeUpdate()
    }
}
