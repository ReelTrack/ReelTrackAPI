package com.example.api.users

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
import kotlin.test.assertTrue

class UserApiTest {

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

    // ─── GET USERS ───────────────────────────────────────────────────────────

    @Test
    fun `GET users - admin can list all users`() = testApplication {
        application { testModule(connection) }

        val adminToken = createUserAndLogin("admin", "admin@test.com", "pass", "ADMIN")
        createUserAndLogin("user1", "user1@test.com", "pass")
        createUserAndLogin("user2", "user2@test.com", "pass")

        val response = client.get("/api/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(body.size >= 3)
    }

    @Test
    fun `GET users - regular user gets 403`() = testApplication {
        application { testModule(connection) }

        val userToken = createUserAndLogin("regular", "regular@test.com", "pass")

        val response = client.get("/api/users") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `GET users id - user can view own profile`() = testApplication {
        application { testModule(connection) }

        val userToken = createUserAndLogin("alice", "alice@test.com", "pass")
        val meResp = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        val userId = Json.parseToJsonElement(meResp.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.int

        val response = client.get("/api/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("alice", body["username"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET users id - user cannot view other user profile`() = testApplication {
        application { testModule(connection) }

        val adminToken = createUserAndLogin("admin2", "admin2@test.com", "pass", "ADMIN")
        val otherToken = createUserAndLogin("bob", "bob@test.com", "pass")
        val userToken  = createUserAndLogin("carol", "carol@test.com", "pass")

        // Get bob's ID
        val bobMeResp = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $otherToken")
        }
        val bobId = Json.parseToJsonElement(bobMeResp.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.int

        val response = client.get("/api/users/$bobId") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    // ─── BAN / UNBAN ─────────────────────────────────────────────────────────

    @Test
    fun `POST ban - moderator can ban user`() = testApplication {
        application { testModule(connection) }

        val modToken  = createUserAndLogin("mod", "mod@test.com", "pass", "MODERATOR")
        val userToken = createUserAndLogin("troublemaker", "trouble@test.com", "pass")

        val meResp = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        val userId = Json.parseToJsonElement(meResp.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.int

        val response = client.post("/api/users/$userId/ban") {
            header(HttpHeaders.Authorization, "Bearer $modToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST ban - banned user cannot login`() = testApplication {
        application { testModule(connection) }

        val modToken  = createUserAndLogin("mod2", "mod2@test.com", "pass", "MODERATOR")
        createUserAndLogin("victim", "victim@test.com", "pass")

        val loginFirst = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"victim@test.com","password":"pass"}""")
        }
        val victimId = Json.parseToJsonElement(loginFirst.bodyAsText())
            .jsonObject["user"]!!.jsonObject["id"]!!.jsonPrimitive.int

        client.post("/api/users/$victimId/ban") {
            header(HttpHeaders.Authorization, "Bearer $modToken")
        }

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"victim@test.com","password":"pass"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST unban - admin can unban user`() = testApplication {
        application { testModule(connection) }

        val adminToken = createUserAndLogin("admin3", "admin3@test.com", "pass", "ADMIN")
        createUserAndLogin("unbanned", "unbanned@test.com", "pass")

        val loginFirst = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"unbanned@test.com","password":"pass"}""")
        }
        val userId = Json.parseToJsonElement(loginFirst.bodyAsText())
            .jsonObject["user"]!!.jsonObject["id"]!!.jsonPrimitive.int

        client.post("/api/users/$userId/ban") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }

        val unbanResp = client.post("/api/users/$userId/unban") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, unbanResp.status)

        val loginAfter = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"unbanned@test.com","password":"pass"}""")
        }
        assertEquals(HttpStatusCode.OK, loginAfter.status)
    }

    @Test
    fun `PUT users id - user can update own profile`() = testApplication {
        application { testModule(connection) }

        val userToken = createUserAndLogin("original", "original@test.com", "pass")
        val meResp = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $userToken")
        }
        val userId = Json.parseToJsonElement(meResp.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.int

        val response = client.put("/api/users/$userId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"username":"updated","email":"updated@test.com","role":"USER","isBanned":false}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE users id - only admin can delete`() = testApplication {
        application { testModule(connection) }

        val adminToken = createUserAndLogin("admin4", "admin4@test.com", "pass", "ADMIN")
        createUserAndLogin("todelete", "todelete@test.com", "pass")

        val loginResp = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"todelete@test.com","password":"pass"}""")
        }
        val userId = Json.parseToJsonElement(loginResp.bodyAsText())
            .jsonObject["user"]!!.jsonObject["id"]!!.jsonPrimitive.int

        val response = client.delete("/api/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

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