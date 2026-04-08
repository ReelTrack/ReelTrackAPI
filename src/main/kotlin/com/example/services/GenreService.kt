package com.example.services

import com.example.models.Genre
import com.example.repositories.GenreRepository

class GenreService(private val genreRepository: GenreRepository) {

    suspend fun createGenre(genre: Genre): Int {
        val existing = genreRepository.findByName(genre.name)
        if (existing != null) {
            throw Exception("Genre with name '${genre.name}' already exists")
        }
        return genreRepository.create(genre)
    }

    suspend fun getGenreById(id: Int): Genre? {
        return genreRepository.findById(id)
    }

    suspend fun getAllGenres(): List<Genre> {
        return genreRepository.findAll()
    }

    suspend fun updateGenre(id: Int, genre: Genre) {
        val existing = genreRepository.findByName(genre.name)
        if (existing != null && existing.id != id) {
            throw Exception("Genre with name '${genre.name}' already exists")
        }
        genreRepository.update(id, genre)
    }

    suspend fun deleteGenre(id: Int) {
        genreRepository.delete(id)
    }
}