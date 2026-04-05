package com.example.controllers

import com.example.config.DatabaseConfig
import com.example.models.*
import com.example.plugins.isAdmin
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.personRoutes() {
    route("/api/persons") {

        // GET /api/persons?search=... — публичный
        get {
            val personService = call.application.attributes[DatabaseConfig.PersonServiceKey]
            val search = call.request.queryParameters["search"]
            val persons = personService.getAllPersons(search)
            call.respond(HttpStatusCode.OK, persons)
        }

        // GET /api/persons/{id} — публичный
        get("/{id}") {
            val personService = call.application.attributes[DatabaseConfig.PersonServiceKey]
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid person ID"))
                return@get
            }

            val person = personService.getPersonById(id)
            if (person != null) {
                call.respond(HttpStatusCode.OK, person)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Person not found"))
            }
        }

        authenticate("auth-jwt") {

            // POST /api/persons — только ADMIN
            post {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@post
                }

                val personService = call.application.attributes[DatabaseConfig.PersonServiceKey]
                val request = call.receive<CreatePersonRequest>()

                try {
                    val person = Person(
                        fullName  = request.fullName,
                        birthDate = request.birthDate,
                        photoUrl  = request.photoUrl
                    )
                    val id = personService.createPerson(person)
                    call.respond(HttpStatusCode.Created, ApiResponse("Person created successfully", id))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // PUT /api/persons/{id} — только ADMIN
            put("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@put
                }

                val personService = call.application.attributes[DatabaseConfig.PersonServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid person ID"))
                    return@put
                }

                val existing = personService.getPersonById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Person not found"))
                    return@put
                }

                val request = call.receive<UpdatePersonRequest>()

                try {
                    val person = Person(
                        fullName  = request.fullName,
                        birthDate = request.birthDate,
                        photoUrl  = request.photoUrl
                    )
                    personService.updatePerson(id, person)
                    call.respond(HttpStatusCode.OK, ApiResponse("Person updated successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }

            // DELETE /api/persons/{id} — только ADMIN
            delete("/{id}") {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied. Admin role required."))
                    return@delete
                }

                val personService = call.application.attributes[DatabaseConfig.PersonServiceKey]
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid person ID"))
                    return@delete
                }

                val existing = personService.getPersonById(id)
                if (existing == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Person not found"))
                    return@delete
                }

                try {
                    personService.deletePerson(id)
                    call.respond(HttpStatusCode.OK, ApiResponse("Person deleted successfully"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Unknown error"))
                }
            }
        }
    }
}