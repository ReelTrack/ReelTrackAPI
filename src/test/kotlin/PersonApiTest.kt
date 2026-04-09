package com.example

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

class PersonApiTest {

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

    @Test
    fun `GET persons - returns empty list initially`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/persons")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0, Json.parseToJsonElement(response.bodyAsText()).jsonArray.size)
    }

    @Test
    fun `POST persons - admin can create person`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()

        val response = client.post("/api/persons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"fullName":"Christopher Nolan","birthDate":"1970-07-30"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertNotNull(Json.parseToJsonElement(response.bodyAsText()).jsonObject["id"])
    }

    @Test
    fun `GET persons - search by name`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()

        client.post("/api/persons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"fullName":"Christopher Nolan"}""")
        }
        client.post("/api/persons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"fullName":"Steven Spielberg"}""")
        }

        val response = client.get("/api/persons?search=nolan")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        assertEquals("Christopher Nolan", body[0].jsonObject["fullName"]?.jsonPrimitive?.content)
    }

    @Test
    fun `PUT persons id - admin can update person`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()

        val createResp = client.post("/api/persons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"fullName":"Old Name"}""")
        }
        val id = Json.parseToJsonElement(createResp.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.int

        val response = client.put("/api/persons/$id") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"fullName":"New Name"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE persons id - admin can delete person`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()

        val createResp = client.post("/api/persons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"fullName":"Temp Person"}""")
        }
        val id = Json.parseToJsonElement(createResp.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.int

        val deleteResp = client.delete("/api/persons/$id") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, deleteResp.status)

        val getResp = client.get("/api/persons/$id")
        assertEquals(HttpStatusCode.NotFound, getResp.status)
    }

    @Test
    fun `POST persons - regular user gets 403`() = testApplication {
        application { testModule(connection) }

        val userToken = createUserAndLogin("u1", "u1@test.com", "pass")

        val response = client.post("/api/persons") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"fullName":"Somebody"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private suspend fun ApplicationTestBuilder.createAdminAndLogin(): String =
        createUserAndLogin("admin", "admin@test.com", "adminpass", "ADMIN")

    private suspend fun ApplicationTestBuilder.createUserAndLogin(
        username: String, email: String, password: String, role: String = "USER"
    ): String {
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","email":"$email","password":"$password","role":"$role"}""")
        }
        val resp = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        return Json.parseToJsonElement(resp.bodyAsText())
            .jsonObject["accessToken"]!!.jsonPrimitive.content
    }
}