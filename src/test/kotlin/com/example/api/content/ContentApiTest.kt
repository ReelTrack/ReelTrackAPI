package com.example.api.content

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
import kotlin.test.assertTrue

class ContentApiTest {

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

    // ─── PUBLIC LIST/GET ─────────────────────────────────────────────────────

    @Test
    fun `GET content - returns empty list initially`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/content")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(0, body.size)
    }

    @Test
    fun `GET content - returns created movies`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        createMovie("Inception", adminToken)
        createMovie("Interstellar", adminToken)

        val response = client.get("/api/content")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun `GET content - filter by type MOVIE`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        createMovie("Movie One", adminToken)
        createSeries("Series One", adminToken)

        val response = client.get("/api/content?type=MOVIE")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        assertEquals("MOVIE", body[0].jsonObject["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET content - filter by type SERIES`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        createMovie("Movie Two", adminToken)
        createSeries("Series Two", adminToken)
        createSeries("Series Three", adminToken)

        val response = client.get("/api/content?type=SERIES")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun `GET content - search by title`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        createMovie("The Dark Knight", adminToken)
        createMovie("Batman Begins", adminToken)
        createMovie("Superman", adminToken)

        val response = client.get("/api/content?search=batman")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        assertEquals("Batman Begins", body[0].jsonObject["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET content - invalid type returns 400`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/content?type=INVALID")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET content id - returns full content detail`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val id = createMovie("Inception", adminToken)

        val response = client.get("/api/content/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Inception", body["title"]?.jsonPrimitive?.content)
        assertEquals("MOVIE", body["type"]?.jsonPrimitive?.content)
        assertNotNull(body["genres"])
        assertNotNull(body["cast"])
    }

    @Test
    fun `GET content id - 404 for nonexistent`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/content/9999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ─── ADMIN CREATE/UPDATE/DELETE ──────────────────────────────────────────

    @Test
    fun `POST content - admin can create movie`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()

        val response = client.post("/api/content") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""
                {
                    "type":"MOVIE",
                    "title":"The Matrix",
                    "description":"A sci-fi classic",
                    "country":"USA",
                    "genreIds":[],
                    "altTitles":[],
                    "languages":[],
                    "cast":[],
                    "staff":[]
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["id"])
    }

    @Test
    fun `POST content - regular user gets 403`() = testApplication {
        application { testModule(connection) }

        val userToken = createUserAndLogin("u1", "u1@test.com", "pass")

        val response = client.post("/api/content") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"type":"MOVIE","title":"X","genreIds":[],"altTitles":[],"languages":[],"cast":[],"staff":[]}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT content id - admin can update`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val id = createMovie("Old Title", adminToken)

        val response = client.put("/api/content/$id") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"type":"MOVIE","title":"New Title","genreIds":[],"altTitles":[],"languages":[],"cast":[],"staff":[]}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val getResp = client.get("/api/content/$id")
        val body = Json.parseToJsonElement(getResp.bodyAsText()).jsonObject
        assertEquals("New Title", body["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `DELETE content id - admin can delete`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val id = createMovie("To Delete", adminToken)

        val deleteResp = client.delete("/api/content/$id") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, deleteResp.status)

        val getResp = client.get("/api/content/$id")
        assertEquals(HttpStatusCode.NotFound, getResp.status)
    }

    // ─── SEASONS ─────────────────────────────────────────────────────────────

    @Test
    fun `POST seasons - admin can add season to series`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val seriesId = createSeries("Breaking Bad", adminToken)

        val response = client.post("/api/content/$seriesId/seasons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"seasonNumber":1,"title":"Season 1"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST seasons - cannot add season to movie`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val movieId = createMovie("Inception", adminToken)

        val response = client.post("/api/content/$movieId/seasons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"seasonNumber":1}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET seasons - public endpoint returns seasons`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val seriesId = createSeries("Game of Thrones", adminToken)

        client.post("/api/content/$seriesId/seasons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"seasonNumber":1,"title":"Season 1"}""")
        }
        client.post("/api/content/$seriesId/seasons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"seasonNumber":2,"title":"Season 2"}""")
        }

        val response = client.get("/api/content/$seriesId/seasons")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
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

    private suspend fun ApplicationTestBuilder.createMovie(title: String, adminToken: String): Int {
        val response = client.post("/api/content") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"type":"MOVIE","title":"$title","genreIds":[],"altTitles":[],"languages":[],"cast":[],"staff":[]}""")
        }
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int
    }

    private suspend fun ApplicationTestBuilder.createSeries(title: String, adminToken: String): Int {
        val response = client.post("/api/content") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"type":"SERIES","title":"$title","genreIds":[],"altTitles":[],"languages":[],"cast":[],"staff":[]}""")
        }
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int
    }
}