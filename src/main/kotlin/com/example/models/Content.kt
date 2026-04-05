package com.example.models

import com.example.utils.LocalDateSerializer
import com.example.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime


@Serializable
enum class ContentType { MOVIE, SERIES }

@Serializable
data class AltTitle(
    val id: Int? = null,
    val contentId: Int? = null,
    val title: String,
    val language: String? = null
)

@Serializable
data class CastEntry(
    val id: Int? = null,
    val contentId: Int? = null,
    val personId: Int,
    val personName: String? = null, // денормализовано для удобства чтения
    val character: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class StaffEntry(
    val id: Int? = null,
    val contentId: Int? = null,
    val personId: Int,
    val personName: String? = null, // денормализовано для удобства чтения
    val role: String
)

// ============================================================
// EPISODE
// ============================================================

@Serializable
data class Episode(
    val id: Int? = null,
    val seasonId: Int? = null,
    val episodeNumber: Int,
    val title: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null
)

@Serializable
data class CreateEpisodeRequest(
    val episodeNumber: Int,
    val title: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null
)

@Serializable
data class UpdateEpisodeRequest(
    val episodeNumber: Int,
    val title: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null
)

// ============================================================
// SEASON
// ============================================================

@Serializable
data class Season(
    val id: Int? = null,
    val contentId: Int? = null,
    val seasonNumber: Int,
    val title: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val description: String? = null,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class CreateSeasonRequest(
    val seasonNumber: Int,
    val title: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val description: String? = null
)

@Serializable
data class UpdateSeasonRequest(
    val seasonNumber: Int,
    val title: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val description: String? = null
)

// ============================================================
// CONTENT
// ============================================================

@Serializable
data class Content(
    val id: Int? = null,
    val type: ContentType,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null,
    val avgRating: Double = 0.0,
    val country: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null
)

// Полный ответ с вложенными данными
@Serializable
data class ContentDetailResponse(
    val id: Int,
    val type: ContentType,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null,
    val avgRating: Double = 0.0,
    val country: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val genres: List<Genre> = emptyList(),
    val altTitles: List<AltTitle> = emptyList(),
    val languages: List<String> = emptyList(),
    val cast: List<CastEntry> = emptyList(),
    val staff: List<StaffEntry> = emptyList(),
    val seasons: List<Season> = emptyList()   // только для SERIES
)

// Краткий ответ для списков
@Serializable
data class ContentListItem(
    val id: Int,
    val type: ContentType,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val posterUrl: String? = null,
    val avgRating: Double = 0.0,
    val country: String? = null,
    val genres: List<Genre> = emptyList()
)

@Serializable
data class CreateContentRequest(
    val type: ContentType,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null,
    val country: String? = null,
    val genreIds: List<Int> = emptyList(),
    val altTitles: List<AltTitle> = emptyList(),
    val languages: List<String> = emptyList(),
    val cast: List<CastEntry> = emptyList(),
    val staff: List<StaffEntry> = emptyList()
)

@Serializable
data class UpdateContentRequest(
    val type: ContentType,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    val director: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val durationMin: Int? = null,
    val description: String? = null,
    val country: String? = null,
    val genreIds: List<Int> = emptyList(),
    val altTitles: List<AltTitle> = emptyList(),
    val languages: List<String> = emptyList(),
    val cast: List<CastEntry> = emptyList(),
    val staff: List<StaffEntry> = emptyList()
)