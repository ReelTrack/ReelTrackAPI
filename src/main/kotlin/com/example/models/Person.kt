package com.example.models

import com.example.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Person(
    val id: Int? = null,
    val fullName: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate? = null,
    val photoUrl: String? = null
)

@Serializable
data class CreatePersonRequest(
    val fullName: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate? = null,
    val photoUrl: String? = null
)

@Serializable
data class UpdatePersonRequest(
    val fullName: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate? = null,
    val photoUrl: String? = null
)