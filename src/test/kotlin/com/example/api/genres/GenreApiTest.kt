package com.example.api.genres

import com.example.testsupport.TestDatabase
import com.example.testsupport.testModule

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GenreApiTest {

    private lateinit var connection: Connection

    @Before
    fun setup() {
        connection = TestDatabase.createConnection()
        TestDatabase.initSchema(connection)
    }

    @After
    fun tearDown() {
        connection.close()
    }

    // ─── PUBLIC ENDPOINTS ────────────────────────────────────────────────────

    @Test
    fun `GET genres - returns empty list initially`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/genres")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(0, body.size)
    }

    @Test
    fun `GET genres - returns created genres`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        createGenre("Action", adminToken)
        createGenre("Drama", adminToken)

        val response = client.get("/api/genres")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun `GET genres id - returns genre by id`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val id = createGenre("Comedy", adminToken)

        val response = client.get("/api/genres/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Comedy", body["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET genres id - 404 for nonexistent`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/genres/9999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ─── ADMIN ENDPOINTS ─────────────────────────────────────────────────────

    @Test
    fun `POST genres - admin can create genre`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()

        val response = client.post("/api/genres") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"name":"Horror"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["id"])
    }

    @Test
    fun `POST genres - regular user gets 403`() = testApplication {
        application { testModule(connection) }

        val userToken = createUserAndLogin("user1", "user1@test.com", "pass123")

        val response = client.post("/api/genres") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"name":"Thriller"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST genres - unauthenticated gets 401`() = testApplication {
        application { testModule(connection) }

        val response = client.post("/api/genres") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Sci-Fi"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST genres - duplicate name returns 409`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        createGenre("Western", adminToken)

        val response = client.post("/api/genres") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"name":"Western"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `PUT genres id - admin can update genre`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val id = createGenre("OldName", adminToken)

        val response = client.put("/api/genres/$id") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"name":"NewName"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val getResponse = client.get("/api/genres/$id")
        val body = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        assertEquals("NewName", body["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `DELETE genres id - admin can delete genre`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val id = createGenre("Temporary", adminToken)

        val deleteResponse = client.delete("/api/genres/$id") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        val getResponse = client.get("/api/genres/$id")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private suspend fun ApplicationTestBuilder.createAdminAndLogin(): String {
        return createUserAndLogin("admin", "admin@test.com", "adminpass", role = "ADMIN")
    }

    private suspend fun ApplicationTestBuilder.createUserAndLogin(
        username: String, email: String, password: String, role: String = "USER"
    ): String {
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","email":"$email","password":"$password","role":"$role"}""")
        }
        val loginResp = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        return Json.parseToJsonElement(loginResp.bodyAsText())
            .jsonObject["accessToken"]!!.jsonPrimitive.content
    }

    private suspend fun ApplicationTestBuilder.createGenre(name: String, adminToken: String): Int {
        val response = client.post("/api/genres") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"name":"$name"}""")
        }
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int
    }
}