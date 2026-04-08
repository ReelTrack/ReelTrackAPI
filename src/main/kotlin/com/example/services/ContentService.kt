package com.example.services

import com.example.models.*
import com.example.repositories.ContentRepository

class ContentService(private val contentRepository: ContentRepository) {

    // ============================================================
    // CONTENT
    // ============================================================

    suspend fun createContent(request: CreateContentRequest): Int {
        val content = Content(
            type        = request.type,
            title       = request.title,
            releaseDate = request.releaseDate,
            posterUrl   = request.posterUrl,
            bannerUrl   = request.bannerUrl,
            durationMin = request.durationMin,
            description = request.description,
            country     = request.country
        )
        val id = contentRepository.create(content)
        contentRepository.setGenres(id, request.genreIds)
        contentRepository.setAltTitles(id, request.altTitles)
        contentRepository.setLanguages(id, request.languages)
        contentRepository.setCast(id, request.cast)
        contentRepository.setStaff(id, request.staff)
        return id
    }

    suspend fun getContentById(id: Int): ContentDetailResponse? {
        val content = contentRepository.findById(id) ?: return null
        return ContentDetailResponse(
            id          = content.id!!,
            type        = content.type,
            title       = content.title,
            releaseDate = content.releaseDate,
            posterUrl   = content.posterUrl,
            bannerUrl   = content.bannerUrl,
            durationMin = content.durationMin,
            description = content.description,
            avgRating   = content.avgRating,
            country     = content.country,
            createdAt   = content.createdAt!!,
            updatedAt   = content.updatedAt!!,
            genres      = contentRepository.findGenresByContentId(content.id),
            altTitles   = contentRepository.findAltTitlesByContentId(content.id),
            languages   = contentRepository.findLanguagesByContentId(content.id),
            cast        = contentRepository.findCastByContentId(content.id),
            staff       = contentRepository.findStaffByContentId(content.id),
            seasons     = if (content.type == ContentType.SERIES)
                contentRepository.findSeasonsByContentId(content.id)
            else emptyList()
        )
    }

    suspend fun getAllContent(
        type: ContentType? = null,
        search: String? = null,
        genreId: Int? = null
    ): List<ContentListItem> {
        return contentRepository.findAll(type, search, genreId)
    }

    suspend fun updateContent(id: Int, request: UpdateContentRequest) {
        val content = Content(
            type        = request.type,
            title       = request.title,
            releaseDate = request.releaseDate,
            posterUrl   = request.posterUrl,
            bannerUrl   = request.bannerUrl,
            durationMin = request.durationMin,
            description = request.description,
            country     = request.country
        )
        contentRepository.update(id, content)
        contentRepository.setGenres(id, request.genreIds)
        contentRepository.setAltTitles(id, request.altTitles)
        contentRepository.setLanguages(id, request.languages)
        contentRepository.setCast(id, request.cast)
        contentRepository.setStaff(id, request.staff)
    }

    suspend fun deleteContent(id: Int) {
        contentRepository.delete(id)
    }

    // ============================================================
    // SEASONS
    // ============================================================

    suspend fun createSeason(contentId: Int, request: CreateSeasonRequest): Int {
        val season = Season(
            seasonNumber = request.seasonNumber,
            title        = request.title,
            releaseDate  = request.releaseDate,
            description  = request.description
        )
        return contentRepository.createSeason(contentId, season)
    }

    suspend fun getSeasonById(id: Int): Season? {
        return contentRepository.findSeasonById(id)
    }

    suspend fun updateSeason(id: Int, request: UpdateSeasonRequest) {
        val season = Season(
            seasonNumber = request.seasonNumber,
            title        = request.title,
            releaseDate  = request.releaseDate,
            description  = request.description
        )
        contentRepository.updateSeason(id, season)
    }

    suspend fun deleteSeason(id: Int) {
        contentRepository.deleteSeason(id)
    }

    // ============================================================
    // EPISODES
    // ============================================================

    suspend fun createEpisode(seasonId: Int, request: CreateEpisodeRequest): Int {
        val episode = Episode(
            episodeNumber = request.episodeNumber,
            title         = request.title,
            releaseDate   = request.releaseDate,
            director      = request.director,
            posterUrl     = request.posterUrl,
            durationMin   = request.durationMin,
            description   = request.description
        )
        return contentRepository.createEpisode(seasonId, episode)
    }

    suspend fun getEpisodeById(id: Int): Episode? {
        return contentRepository.findEpisodeById(id)
    }

    suspend fun updateEpisode(id: Int, request: UpdateEpisodeRequest) {
        val episode = Episode(
            episodeNumber = request.episodeNumber,
            title         = request.title,
            releaseDate   = request.releaseDate,
            director      = request.director,
            posterUrl     = request.posterUrl,
            durationMin   = request.durationMin,
            description   = request.description
        )
        contentRepository.updateEpisode(id, episode)
    }

    suspend fun deleteEpisode(id: Int) {
        contentRepository.deleteEpisode(id)
    }
}