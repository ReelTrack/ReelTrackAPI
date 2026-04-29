package com.example.testsupport

import com.example.repositories.*
import com.example.services.*
import java.sql.Connection
import java.sql.DriverManager

object TestDatabase {

    fun createConnection(): Connection {
        Class.forName("org.h2.Driver")
        val conn = DriverManager.getConnection(
            "jdbc:h2:mem:reeltrack;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
            "sa", ""
        )
        conn.autoCommit = true
        return conn
    }

    fun initSchema(connection: Connection) {
        connection.createStatement().execute("DROP ALL OBJECTS")

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(255) NOT NULL UNIQUE,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL DEFAULT 'USER',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_banned BOOLEAN NOT NULL DEFAULT FALSE
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS tokens (
                id SERIAL PRIMARY KEY,
                user_id INTEGER NOT NULL,
                token TEXT NOT NULL UNIQUE,
                refresh_token TEXT NOT NULL UNIQUE,
                expires_at TIMESTAMP NOT NULL,
                refresh_expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS genres (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL UNIQUE
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS persons (
                id SERIAL PRIMARY KEY,
                full_name VARCHAR(255) NOT NULL,
                birth_date DATE,
                photo_url TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS content (
                id SERIAL PRIMARY KEY,
                type VARCHAR(10) NOT NULL CHECK (type IN ('MOVIE', 'SERIES')),
                title VARCHAR(500) NOT NULL,
                release_date DATE,
                director VARCHAR(255),
                poster_url TEXT,
                banner_url TEXT,
                duration_min INTEGER,
                description TEXT,
                avg_rating NUMERIC(3,1) DEFAULT 0.0,
                country VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS content_genres (
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                genre_id   INTEGER NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                PRIMARY KEY (content_id, genre_id)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS content_alt_titles (
                id SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                title VARCHAR(500) NOT NULL,
                language VARCHAR(10)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS content_languages (
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                language VARCHAR(10) NOT NULL,
                PRIMARY KEY (content_id, language)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS content_cast (
                id SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                person_id  INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
                character VARCHAR(255),
                sort_order INTEGER DEFAULT 0
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS content_staff (
                id SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                person_id  INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
                role VARCHAR(100) NOT NULL
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS seasons (
                id SERIAL PRIMARY KEY,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                season_number INTEGER NOT NULL,
                title VARCHAR(255),
                release_date DATE,
                description TEXT,
                UNIQUE (content_id, season_number)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS episodes (
                id SERIAL PRIMARY KEY,
                season_id INTEGER NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
                episode_number INTEGER NOT NULL,
                title VARCHAR(500),
                release_date DATE,
                director VARCHAR(255),
                poster_url TEXT,
                duration_min INTEGER,
                description TEXT,
                UNIQUE (season_id, episode_number)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS reviews (
                id SERIAL PRIMARY KEY,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
                rating SMALLINT CHECK (rating BETWEEN 1 AND 10),
                body TEXT,
                is_spoiler BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (user_id, content_id)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS review_likes (
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                review_id INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, review_id)
            )
        """)

        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS review_comments (
                id SERIAL PRIMARY KEY,
                review_id INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                parent_id INTEGER REFERENCES review_comments(id) ON DELETE CASCADE,
                body TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)
    }
}