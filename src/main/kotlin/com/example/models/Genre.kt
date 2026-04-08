package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Int? = null,
    val name: String
)

@Serializable
data class CreateGenreRequest(
    val name: String
)

@Serializable
data class UpdateGenreRequest(
    val name: String
)
