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
import kotlin.test.assertTrue

class AuthApiTest {

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

    // ─── REGISTER ────────────────────────────────────────────────────────────

    @Test
    fun `POST register - success`() = testApplication {
        application { testModule(connection) }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","email":"alice@test.com","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("User registered successfully", body["message"]?.jsonPrimitive?.content)
        assertNotNull(body["id"])
    }

    @Test
    fun `POST register - duplicate email returns 409`() = testApplication {
        application { testModule(connection) }

        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","email":"alice@test.com","password":"secret123"}""")
        }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice2","email":"alice@test.com","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST register - missing password returns 400`() = testApplication {
        application { testModule(connection) }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"bob","email":"bob@test.com"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    @Test
    fun `POST login - success returns tokens`() = testApplication {
        application { testModule(connection) }

        registerUser("charlie", "charlie@test.com", "pass123")

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"charlie@test.com","password":"pass123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["accessToken"])
        assertNotNull(body["refreshToken"])
        assertNotNull(body["user"])
    }

    @Test
    fun `POST login - wrong password returns 401`() = testApplication {
        application { testModule(connection) }

        registerUser("dave", "dave@test.com", "correct")

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"dave@test.com","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST login - unknown email returns 401`() = testApplication {
        application { testModule(connection) }

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"nobody@test.com","password":"pass"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ─── ME ──────────────────────────────────────────────────────────────────

    @Test
    fun `GET me - valid token returns user`() = testApplication {
        application { testModule(connection) }

        registerUser("eve", "eve@test.com", "pass123")
        val token = loginAndGetToken("eve@test.com", "pass123")

        val response = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("eve", body["username"]?.jsonPrimitive?.content)
        assertEquals("eve@test.com", body["email"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET me - no token returns 401`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/auth/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET me - invalid token returns 401`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer this.is.fake")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @Test
    fun `POST logout - success`() = testApplication {
        application { testModule(connection) }

        registerUser("frank", "frank@test.com", "pass123")
        val token = loginAndGetToken("frank@test.com", "pass123")

        val response = client.post("/api/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST logout - after logout token is invalid`() = testApplication {
        application { testModule(connection) }

        registerUser("grace", "grace@test.com", "pass123")
        val token = loginAndGetToken("grace@test.com", "pass123")

        client.post("/api/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // После логаута токен должен быть невалиден для /me
        val response = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ─── REFRESH ─────────────────────────────────────────────────────────────

    @Test
    fun `POST refresh - success returns new tokens`() = testApplication {
        application { testModule(connection) }

        registerUser("heidi", "heidi@test.com", "pass123")
        val (_, refreshToken) = loginAndGetTokens("heidi@test.com", "pass123")

        val response = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["accessToken"])
        assertNotNull(body["refreshToken"])
    }

    @Test
    fun `POST refresh - invalid token returns 401`() = testApplication {
        application { testModule(connection) }

        val response = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"not.a.valid.token"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private suspend fun ApplicationTestBuilder.registerUser(
        username: String, email: String, password: String
    ) {
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","email":"$email","password":"$password"}""")
        }
    }

    private suspend fun ApplicationTestBuilder.loginAndGetToken(
        email: String, password: String
    ): String {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["accessToken"]!!.jsonPrimitive.content
    }

    private suspend fun ApplicationTestBuilder.loginAndGetTokens(
        email: String, password: String
    ): Pair<String, String> {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return Pair(
            body["accessToken"]!!.jsonPrimitive.content,
            body["refreshToken"]!!.jsonPrimitive.content
        )
    }
}