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

class ReviewApiTest {

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

    // ─── CREATE REVIEW ───────────────────────────────────────────────────────

    @Test
    fun `POST reviews - user can create review with rating`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Inception", adminToken)
        val userToken = createUserAndLogin("reviewer", "reviewer@test.com", "pass123")

        val response = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"rating":9,"body":"Awesome movie!","isSpoiler":false}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST reviews - body only review is valid`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Interstellar", adminToken)
        val userToken = createUserAndLogin("u2", "u2@test.com", "pass")

        val response = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"body":"Just a text review","isSpoiler":false}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST reviews - empty rating and body returns 400`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Dunkirk", adminToken)
        val userToken = createUserAndLogin("u3", "u3@test.com", "pass")

        val response = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"isSpoiler":false}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST reviews - unauthenticated returns 401`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Tenet", adminToken)

        val response = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            setBody("""{"rating":7}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST reviews - duplicate review for same content returns 409`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Oppenheimer", adminToken)
        val userToken = createUserAndLogin("u4", "u4@test.com", "pass")

        client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"rating":8}""")
        }

        val response = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"rating":9}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    // ─── GET REVIEWS ─────────────────────────────────────────────────────────

    @Test
    fun `GET reviews by content - public endpoint returns list`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("The Godfather", adminToken)
        val u1 = createUserAndLogin("u5", "u5@test.com", "pass")
        val u2 = createUserAndLogin("u6", "u6@test.com", "pass")

        client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $u1")
            setBody("""{"rating":10}""")
        }
        client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $u2")
            setBody("""{"rating":9,"body":"Classic!"}""")
        }

        val response = client.get("/api/content/$contentId/reviews")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun `GET review by id - returns review with likes and comments`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Parasite", adminToken)
        val userToken = createUserAndLogin("u7", "u7@test.com", "pass")

        val createResp = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"rating":10,"body":"Masterpiece"}""")
        }
        val reviewId = Json.parseToJsonElement(createResp.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int

        val response = client.get("/api/reviews/$reviewId")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(10, body["rating"]?.jsonPrimitive?.int)
        assertNotNull(body["likes"])
        assertNotNull(body["comments"])
    }

    @Test
    fun `GET review - 404 for nonexistent`() = testApplication {
        application { testModule(connection) }

        val response = client.get("/api/reviews/9999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ─── UPDATE / DELETE ─────────────────────────────────────────────────────

    @Test
    fun `PUT review - owner can update`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Whiplash", adminToken)
        val userToken = createUserAndLogin("u8", "u8@test.com", "pass")
        val reviewId = createReview(contentId, userToken, 7)

        val response = client.put("/api/reviews/$reviewId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"rating":9,"body":"Actually even better!","isSpoiler":false}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT review - other user gets 403`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("1917", adminToken)
        val ownerToken = createUserAndLogin("owner", "owner@test.com", "pass")
        val otherToken = createUserAndLogin("other", "other@test.com", "pass")
        val reviewId = createReview(contentId, ownerToken, 8)

        val response = client.put("/api/reviews/$reviewId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $otherToken")
            setBody("""{"rating":3,"isSpoiler":false}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE review - moderator can delete any review`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Arrival", adminToken)
        val userToken = createUserAndLogin("u9", "u9@test.com", "pass")
        val modToken  = createUserAndLogin("mod", "mod@test.com", "pass", "MODERATOR")
        val reviewId = createReview(contentId, userToken, 9)

        val response = client.delete("/api/reviews/$reviewId") {
            header(HttpHeaders.Authorization, "Bearer $modToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ─── LIKES ───────────────────────────────────────────────────────────────

    @Test
    fun `POST like - toggle adds like`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Her", adminToken)
        val reviewer  = createUserAndLogin("u10", "u10@test.com", "pass")
        val liker     = createUserAndLogin("u11", "u11@test.com", "pass")
        val reviewId = createReview(contentId, reviewer, 8)

        val response = client.post("/api/reviews/$reviewId/like") {
            header(HttpHeaders.Authorization, "Bearer $liker")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(true,  body["liked"]?.jsonPrimitive?.boolean)
        assertEquals(1,     body["likesCount"]?.jsonPrimitive?.int)
    }

    @Test
    fun `POST like - toggle twice removes like`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Drive", adminToken)
        val reviewer  = createUserAndLogin("u12", "u12@test.com", "pass")
        val liker     = createUserAndLogin("u13", "u13@test.com", "pass")
        val reviewId = createReview(contentId, reviewer, 8)

        client.post("/api/reviews/$reviewId/like") {
            header(HttpHeaders.Authorization, "Bearer $liker")
        }
        val response = client.post("/api/reviews/$reviewId/like") {
            header(HttpHeaders.Authorization, "Bearer $liker")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(false, body["liked"]?.jsonPrimitive?.boolean)
        assertEquals(0,     body["likesCount"]?.jsonPrimitive?.int)
    }

    // ─── COMMENTS ────────────────────────────────────────────────────────────

    @Test
    fun `POST comment - user can comment on review`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Joker", adminToken)
        val reviewer  = createUserAndLogin("u14", "u14@test.com", "pass")
        val commenter = createUserAndLogin("u15", "u15@test.com", "pass")
        val reviewId = createReview(contentId, reviewer, 7)

        val response = client.post("/api/reviews/$reviewId/comments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $commenter")
            setBody("""{"body":"Great review!"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET comments - public endpoint returns list`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Shutter Island", adminToken)
        val reviewer  = createUserAndLogin("u16", "u16@test.com", "pass")
        val commenter = createUserAndLogin("u17", "u17@test.com", "pass")
        val reviewId = createReview(contentId, reviewer, 8)

        client.post("/api/reviews/$reviewId/comments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $commenter")
            setBody("""{"body":"Comment 1"}""")
        }
        client.post("/api/reviews/$reviewId/comments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $commenter")
            setBody("""{"body":"Comment 2"}""")
        }

        val response = client.get("/api/reviews/$reviewId/comments")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun `DELETE comment - owner can delete own comment`() = testApplication {
        application { testModule(connection) }

        val adminToken = createAdminAndLogin()
        val contentId = createMovie("Memento", adminToken)
        val reviewer  = createUserAndLogin("u18", "u18@test.com", "pass")
        val commenter = createUserAndLogin("u19", "u19@test.com", "pass")
        val reviewId = createReview(contentId, reviewer, 8)

        val commentResp = client.post("/api/reviews/$reviewId/comments") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $commenter")
            setBody("""{"body":"To be deleted"}""")
        }
        val commentId = Json.parseToJsonElement(commentResp.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int

        val response = client.delete("/api/comments/$commentId") {
            header(HttpHeaders.Authorization, "Bearer $commenter")
        }
        assertEquals(HttpStatusCode.OK, response.status)
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

    private suspend fun ApplicationTestBuilder.createMovie(title: String, adminToken: String): Int {
        val response = client.post("/api/content") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            setBody("""{"type":"MOVIE","title":"$title","genreIds":[],"altTitles":[],"languages":[],"cast":[],"staff":[]}""")
        }
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int
    }

    private suspend fun ApplicationTestBuilder.createReview(
        contentId: Int, userToken: String, rating: Int
    ): Int {
        val response = client.post("/api/content/$contentId/reviews") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $userToken")
            setBody("""{"rating":$rating,"isSpoiler":false}""")
        }
        return Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.int
    }
}