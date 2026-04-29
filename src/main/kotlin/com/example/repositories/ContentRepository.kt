package com.example.repositories

import com.example.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Date
import java.sql.Statement

class ContentRepository(private val connection: Connection) {

    fun createTableIfNotExists() {
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS content (
                id           SERIAL PRIMARY KEY,
                type         VARCHAR(10)  NOT NULL CHECK (type IN ('MOVIE', 'SERIES')),
                title        VARCHAR(500) NOT NULL,
                release_date DATE,
                director     VARCHAR(255),
                poster_url   TEXT,
                banner_url   TEXT,
                duration_min INTEGER,
                description  TEXT,
                avg_rating   NUMERIC(3,1) DEFAULT 0.0,
                country      VARCHAR(100),
                created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS content_alt_titles (
                id         SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                title      VARCHAR(500) NOT NULL,
                language   VARCHAR(10)
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS content_languages (
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                language   VARCHAR(10) NOT NULL,
                PRIMARY KEY (content_id, language)
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS content_genres (
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                genre_id   INTEGER NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                PRIMARY KEY (content_id, genre_id)
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS content_cast (
                id         SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                person_id  INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
                character  VARCHAR(255),
                sort_order INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS content_staff (
                id         SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                person_id  INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
                role       VARCHAR(100) NOT NULL
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS seasons (
                id            SERIAL PRIMARY KEY,
                content_id    INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                season_number INTEGER NOT NULL,
                title         VARCHAR(255),
                release_date  DATE,
                description   TEXT,
                UNIQUE (content_id, season_number)
            )
            """.trimIndent()
        )
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS episodes (
                id             SERIAL PRIMARY KEY,
                season_id      INTEGER NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
                episode_number INTEGER NOT NULL,
                title          VARCHAR(500),
                release_date   DATE,
                director       VARCHAR(255),
                poster_url     TEXT,
                duration_min   INTEGER,
                description    TEXT,
                UNIQUE (season_id, episode_number)
            )
            """.trimIndent()
        )
    }

    // ============================================================
    // CONTENT CRUD
    // ============================================================

    suspend fun create(content: Content): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            INSERT INTO content (type, title, release_date, poster_url, banner_url,
                duration_min, description, country)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            Statement.RETURN_GENERATED_KEYS
        )
        stmt.setString(1, content.type.name)
        stmt.setString(2, content.title)
        stmt.setDate(3, content.releaseDate?.let { Date.valueOf(it) })
        stmt.setString(4, content.posterUrl)
        stmt.setString(5, content.bannerUrl)
        stmt.setObject(6, content.durationMin)
        stmt.setString(7, content.description)
        stmt.setString(8, content.country)
        stmt.executeUpdate()
        val rs = stmt.generatedKeys
        if (rs.next()) rs.getInt("id") else throw Exception("Failed to create content")
    }

    suspend fun findById(id: Int): Content? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT * FROM content WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toContent() else null
    }

    suspend fun findAll(
        type: ContentType? = null,
        search: String? = null,
        genreId: Int? = null
    ): List<ContentListItem> = withContext(Dispatchers.IO) {
        val conditions = mutableListOf<String>()
        if (type != null) conditions += "c.type = ?"
        if (search != null) conditions += "LOWER(c.title) LIKE LOWER(?)"
        if (genreId != null) conditions += "EXISTS (SELECT 1 FROM content_genres cg WHERE cg.content_id = c.id AND cg.genre_id = ?)"

        val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"

        val stmt = connection.prepareStatement(
            """
            SELECT c.id, c.type, c.title, c.release_date, c.poster_url, c.avg_rating, c.country
            FROM content c
            $where
            ORDER BY c.title
            """.trimIndent()
        )

        var idx = 1
        if (type != null) stmt.setString(idx++, type.name)
        if (search != null) stmt.setString(idx++, "%$search%")
        if (genreId != null) stmt.setInt(idx, genreId)

        val rs = stmt.executeQuery()
        val items = mutableListOf<ContentListItem>()
        while (rs.next()) {
            val contentId = rs.getInt("id")
            items += ContentListItem(
                id          = contentId,
                type        = ContentType.valueOf(rs.getString("type")),
                title       = rs.getString("title"),
                releaseDate = rs.getDate("release_date")?.toLocalDate(),
                posterUrl   = rs.getString("poster_url"),
                avgRating   = rs.getDouble("avg_rating"),
                country     = rs.getString("country"),
                genres      = findGenresByContentId(contentId)
            )
        }
        items
    }

    suspend fun update(id: Int, content: Content) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            UPDATE content SET type = ?, title = ?, release_date = ?,
                poster_url = ?, banner_url = ?, duration_min = ?, description = ?,
                country = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """.trimIndent()
        )
        stmt.setString(1, content.type.name)
        stmt.setString(2, content.title)
        stmt.setDate(3, content.releaseDate?.let { Date.valueOf(it) })
        stmt.setString(4, content.posterUrl)
        stmt.setString(5, content.bannerUrl)
        stmt.setObject(6, content.durationMin)
        stmt.setString(7, content.description)
        stmt.setString(8, content.country)
        stmt.setInt(9, id)
        stmt.executeUpdate()
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("DELETE FROM content WHERE id = ?")
        stmt.setInt(1, id)
        stmt.executeUpdate()
    }

    // ============================================================
    // GENRES
    // ============================================================

    fun findGenresByContentId(contentId: Int): List<Genre> {
        val stmt = connection.prepareStatement(
            "SELECT g.id, g.name FROM genres g JOIN content_genres cg ON g.id = cg.genre_id WHERE cg.content_id = ?"
        )
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<Genre>()
        while (rs.next()) result += Genre(id = rs.getInt("id"), name = rs.getString("name"))
        return result
    }

    suspend fun setGenres(contentId: Int, genreIds: List<Int>) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM content_genres WHERE content_id = ?")
            .also { it.setInt(1, contentId) }.executeUpdate()
        if (genreIds.isEmpty()) return@withContext
        val stmt = connection.prepareStatement("INSERT INTO content_genres (content_id, genre_id) VALUES (?, ?)")
        for (gid in genreIds) {
            stmt.setInt(1, contentId)
            stmt.setInt(2, gid)
            stmt.addBatch()
        }
        stmt.executeBatch()
    }

    // ============================================================
    // ALT TITLES
    // ============================================================

    fun findAltTitlesByContentId(contentId: Int): List<AltTitle> {
        val stmt = connection.prepareStatement("SELECT * FROM content_alt_titles WHERE content_id = ?")
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<AltTitle>()
        while (rs.next()) result += AltTitle(
            id        = rs.getInt("id"),
            contentId = contentId,
            title     = rs.getString("title"),
            language  = rs.getString("language")
        )
        return result
    }

    suspend fun setAltTitles(contentId: Int, altTitles: List<AltTitle>) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM content_alt_titles WHERE content_id = ?")
            .also { it.setInt(1, contentId) }.executeUpdate()
        if (altTitles.isEmpty()) return@withContext
        val stmt = connection.prepareStatement(
            "INSERT INTO content_alt_titles (content_id, title, language) VALUES (?, ?, ?)"
        )
        for (alt in altTitles) {
            stmt.setInt(1, contentId)
            stmt.setString(2, alt.title)
            stmt.setString(3, alt.language)
            stmt.addBatch()
        }
        stmt.executeBatch()
    }

    // ============================================================
    // LANGUAGES
    // ============================================================

    fun findLanguagesByContentId(contentId: Int): List<String> {
        val stmt = connection.prepareStatement("SELECT language FROM content_languages WHERE content_id = ?")
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<String>()
        while (rs.next()) result += rs.getString("language")
        return result
    }

    suspend fun setLanguages(contentId: Int, languages: List<String>) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM content_languages WHERE content_id = ?")
            .also { it.setInt(1, contentId) }.executeUpdate()
        if (languages.isEmpty()) return@withContext
        val stmt = connection.prepareStatement(
            "INSERT INTO content_languages (content_id, language) VALUES (?, ?)"
        )
        for (lang in languages) {
            stmt.setInt(1, contentId)
            stmt.setString(2, lang)
            stmt.addBatch()
        }
        stmt.executeBatch()
    }

    // ============================================================
    // CAST
    // ============================================================

    fun findCastByContentId(contentId: Int): List<CastEntry> {
        val stmt = connection.prepareStatement(
            """
            SELECT cc.id, cc.content_id, cc.person_id, p.full_name, cc.character, cc.sort_order
            FROM content_cast cc JOIN persons p ON cc.person_id = p.id
            WHERE cc.content_id = ? ORDER BY cc.sort_order
            """.trimIndent()
        )
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<CastEntry>()
        while (rs.next()) result += CastEntry(
            id         = rs.getInt("id"),
            contentId  = contentId,
            personId   = rs.getInt("person_id"),
            personName = rs.getString("full_name"),
            character  = rs.getString("character"),
            sortOrder  = rs.getInt("sort_order")
        )
        return result
    }

    suspend fun setCast(contentId: Int, cast: List<CastEntry>) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM content_cast WHERE content_id = ?")
            .also { it.setInt(1, contentId) }.executeUpdate()
        if (cast.isEmpty()) return@withContext
        val stmt = connection.prepareStatement(
            "INSERT INTO content_cast (content_id, person_id, character, sort_order) VALUES (?, ?, ?, ?)"
        )
        for (entry in cast) {
            stmt.setInt(1, contentId)
            stmt.setInt(2, entry.personId)
            stmt.setString(3, entry.character)
            stmt.setInt(4, entry.sortOrder)
            stmt.addBatch()
        }
        stmt.executeBatch()
    }

    // ============================================================
    // STAFF
    // ============================================================

    fun findStaffByContentId(contentId: Int): List<StaffEntry> {
        val stmt = connection.prepareStatement(
            """
            SELECT cs.id, cs.content_id, cs.person_id, p.full_name, cs.role
            FROM content_staff cs JOIN persons p ON cs.person_id = p.id
            WHERE cs.content_id = ?
            """.trimIndent()
        )
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<StaffEntry>()
        while (rs.next()) result += StaffEntry(
            id         = rs.getInt("id"),
            contentId  = contentId,
            personId   = rs.getInt("person_id"),
            personName = rs.getString("full_name"),
            role       = rs.getString("role")
        )
        return result
    }

    suspend fun setStaff(contentId: Int, staff: List<StaffEntry>) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM content_staff WHERE content_id = ?")
            .also { it.setInt(1, contentId) }.executeUpdate()
        if (staff.isEmpty()) return@withContext
        val stmt = connection.prepareStatement(
            "INSERT INTO content_staff (content_id, person_id, role) VALUES (?, ?, ?)"
        )
        for (entry in staff) {
            stmt.setInt(1, contentId)
            stmt.setInt(2, entry.personId)
            stmt.setString(3, entry.role)
            stmt.addBatch()
        }
        stmt.executeBatch()
    }

    // ============================================================
    // SEASONS
    // ============================================================

    suspend fun findSeasonsByContentId(contentId: Int): List<Season> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT * FROM seasons WHERE content_id = ? ORDER BY season_number"
        )
        stmt.setInt(1, contentId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<Season>()
        while (rs.next()) {
            val seasonId = rs.getInt("id")
            result += Season(
                id           = seasonId,
                contentId    = contentId,
                seasonNumber = rs.getInt("season_number"),
                title        = rs.getString("title"),
                releaseDate  = rs.getDate("release_date")?.toLocalDate(),
                description  = rs.getString("description"),
                episodes     = findEpisodesBySeasonId(seasonId)
            )
        }
        result
    }

    suspend fun createSeason(contentId: Int, season: Season): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "INSERT INTO seasons (content_id, season_number, title, release_date, description) VALUES (?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        )
        stmt.setInt(1, contentId)
        stmt.setInt(2, season.seasonNumber)
        stmt.setString(3, season.title)
        stmt.setDate(4, season.releaseDate?.let { Date.valueOf(it) })
        stmt.setString(5, season.description)
        stmt.executeUpdate()
        val rs = stmt.generatedKeys
        if (rs.next()) rs.getInt("id") else throw Exception("Failed to create season")
    }

    suspend fun findSeasonById(id: Int): Season? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT * FROM seasons WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (!rs.next()) return@withContext null
        val seasonId = rs.getInt("id")
        Season(
            id           = seasonId,
            contentId    = rs.getInt("content_id"),
            seasonNumber = rs.getInt("season_number"),
            title        = rs.getString("title"),
            releaseDate  = rs.getDate("release_date")?.toLocalDate(),
            description  = rs.getString("description"),
            episodes     = findEpisodesBySeasonId(seasonId)
        )
    }

    suspend fun updateSeason(id: Int, season: Season) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "UPDATE seasons SET season_number = ?, title = ?, release_date = ?, description = ? WHERE id = ?"
        )
        stmt.setInt(1, season.seasonNumber)
        stmt.setString(2, season.title)
        stmt.setDate(3, season.releaseDate?.let { Date.valueOf(it) })
        stmt.setString(4, season.description)
        stmt.setInt(5, id)
        stmt.executeUpdate()
    }

    suspend fun deleteSeason(id: Int) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM seasons WHERE id = ?")
            .also { it.setInt(1, id) }.executeUpdate()
    }

    // ============================================================
    // EPISODES
    // ============================================================

    fun findEpisodesBySeasonId(seasonId: Int): List<Episode> {
        val stmt = connection.prepareStatement(
            "SELECT * FROM episodes WHERE season_id = ? ORDER BY episode_number"
        )
        stmt.setInt(1, seasonId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<Episode>()
        while (rs.next()) result += rs.toEpisode()
        return result
    }

    suspend fun createEpisode(seasonId: Int, episode: Episode): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            INSERT INTO episodes (season_id, episode_number, title, release_date, director, poster_url, duration_min, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            Statement.RETURN_GENERATED_KEYS
        )
        stmt.setInt(1, seasonId)
        stmt.setInt(2, episode.episodeNumber)
        stmt.setString(3, episode.title)
        stmt.setDate(4, episode.releaseDate?.let { Date.valueOf(it) })
        stmt.setString(5, episode.director)
        stmt.setString(6, episode.posterUrl)
        stmt.setObject(7, episode.durationMin)
        stmt.setString(8, episode.description)
        stmt.executeUpdate()
        val rs = stmt.generatedKeys
        if (rs.next()) rs.getInt("id") else throw Exception("Failed to create episode")
    }

    suspend fun findEpisodeById(id: Int): Episode? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT * FROM episodes WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toEpisode() else null
    }

    suspend fun updateEpisode(id: Int, episode: Episode) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            """
            UPDATE episodes SET episode_number = ?, title = ?, release_date = ?,
                director = ?, poster_url = ?, duration_min = ?, description = ?
            WHERE id = ?
            """.trimIndent()
        )
        stmt.setInt(1, episode.episodeNumber)
        stmt.setString(2, episode.title)
        stmt.setDate(3, episode.releaseDate?.let { Date.valueOf(it) })
        stmt.setString(4, episode.director)
        stmt.setString(5, episode.posterUrl)
        stmt.setObject(6, episode.durationMin)
        stmt.setString(7, episode.description)
        stmt.setInt(8, id)
        stmt.executeUpdate()
    }

    suspend fun deleteEpisode(id: Int) = withContext(Dispatchers.IO) {
        connection.prepareStatement("DELETE FROM episodes WHERE id = ?")
            .also { it.setInt(1, id) }.executeUpdate()
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun java.sql.ResultSet.toContent() = Content(
        id          = getInt("id"),
        type        = ContentType.valueOf(getString("type")),
        title       = getString("title"),
        releaseDate = getDate("release_date")?.toLocalDate(),
        posterUrl   = getString("poster_url"),
        bannerUrl   = getString("banner_url"),
        durationMin = getObject("duration_min") as? Int,
        description = getString("description"),
        avgRating   = getDouble("avg_rating"),
        country     = getString("country"),
        createdAt   = getTimestamp("created_at")?.toLocalDateTime(),
        updatedAt   = getTimestamp("updated_at")?.toLocalDateTime()
    )

    private fun java.sql.ResultSet.toEpisode() = Episode(
        id            = getInt("id"),
        seasonId      = getInt("season_id"),
        episodeNumber = getInt("episode_number"),
        title         = getString("title"),
        releaseDate   = getDate("release_date")?.toLocalDate(),
        director      = getString("director"),
        posterUrl     = getString("poster_url"),
        durationMin   = getObject("duration_min") as? Int,
        description   = getString("description")
    )
}