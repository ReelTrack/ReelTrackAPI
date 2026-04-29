package com.example.repositories

import com.example.models.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Date
import java.sql.Statement

class PersonRepository(private val connection: Connection) {

    fun createTableIfNotExists() {
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS persons (
                id         SERIAL PRIMARY KEY,
                full_name  VARCHAR(255) NOT NULL,
                birth_date DATE,
                photo_url  TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
    }

    suspend fun create(person: Person): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "INSERT INTO persons (full_name, birth_date, photo_url) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        )
        stmt.setString(1, person.fullName)
        stmt.setDate(2, person.birthDate?.let { Date.valueOf(it) })
        stmt.setString(3, person.photoUrl)
        stmt.executeUpdate()
        val rs = stmt.generatedKeys
        if (rs.next()) rs.getInt("id")
        else throw Exception("Failed to create person")
    }

    suspend fun findById(id: Int): Person? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT * FROM persons WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toPerson() else null
    }

    suspend fun findAll(search: String? = null): List<Person> = withContext(Dispatchers.IO) {
        val sql = if (search != null)
            "SELECT * FROM persons WHERE LOWER(full_name) LIKE LOWER(?) ORDER BY full_name"
        else
            "SELECT * FROM persons ORDER BY full_name"

        val stmt = connection.prepareStatement(sql)
        if (search != null) stmt.setString(1, "%$search%")

        val rs = stmt.executeQuery()
        val result = mutableListOf<Person>()
        while (rs.next()) result += rs.toPerson()
        result
    }

    suspend fun update(id: Int, person: Person) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "UPDATE persons SET full_name = ?, birth_date = ?, photo_url = ? WHERE id = ?"
        )
        stmt.setString(1, person.fullName)
        stmt.setDate(2, person.birthDate?.let { Date.valueOf(it) })
        stmt.setString(3, person.photoUrl)
        stmt.setInt(4, id)
        stmt.executeUpdate()
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("DELETE FROM persons WHERE id = ?")
        stmt.setInt(1, id)
        stmt.executeUpdate()
    }

    private fun java.sql.ResultSet.toPerson() = Person(
        id        = getInt("id"),
        fullName  = getString("full_name"),
        birthDate = getDate("birth_date")?.toLocalDate(),
        photoUrl  = getString("photo_url")
    )
}