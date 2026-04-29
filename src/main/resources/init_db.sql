-- CREATE DATABASE "ReelTrackDB";

-- Connect to ReelTrackDB and create tables

-- ============================================================
-- ПОЛЬЗОВАТЕЛИ И АВТОРИЗАЦИЯ
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_banned     BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tokens (
    id                 SERIAL PRIMARY KEY,
    user_id            INTEGER NOT NULL,
    token              TEXT NOT NULL UNIQUE,
    refresh_token      TEXT NOT NULL UNIQUE,
    expires_at         TIMESTAMP NOT NULL,
    refresh_expires_at TIMESTAMP NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- СПРАВОЧНИКИ
-- ============================================================

CREATE TABLE IF NOT EXISTS genres (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS persons (
    id         SERIAL PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    birth_date DATE,
    photo_url  TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- КОНТЕНТ (ФИЛЬМЫ И СЕРИАЛЫ)
-- ============================================================

CREATE TABLE IF NOT EXISTS content (
    id           SERIAL PRIMARY KEY,
    type         VARCHAR(10)  NOT NULL CHECK (type IN ('MOVIE', 'SERIES')),
    title        VARCHAR(500) NOT NULL,
    release_date DATE,
    poster_url   TEXT,
    banner_url   TEXT,
    duration_min INTEGER,
    description  TEXT,
    avg_rating   NUMERIC(3,1) DEFAULT 0.0,
    country      VARCHAR(100),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_alt_titles (
    id         SERIAL PRIMARY KEY,
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    title      VARCHAR(500) NOT NULL,
    language   VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS content_languages (
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    language   VARCHAR(10) NOT NULL,
    PRIMARY KEY (content_id, language)
);

CREATE TABLE IF NOT EXISTS content_genres (
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    genre_id   INTEGER NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (content_id, genre_id)
);

CREATE TABLE IF NOT EXISTS content_cast (
    id         SERIAL PRIMARY KEY,
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    person_id  INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    character  VARCHAR(255),
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS content_staff (
    id         SERIAL PRIMARY KEY,
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    person_id  INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    role       VARCHAR(100) NOT NULL
);

-- ============================================================
-- СЕЗОНЫ И ЭПИЗОДЫ (только для SERIES)
-- ============================================================

CREATE TABLE IF NOT EXISTS seasons (
    id            SERIAL PRIMARY KEY,
    content_id    INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    season_number INTEGER NOT NULL,
    title         VARCHAR(255),
    release_date  DATE,
    description   TEXT,
    UNIQUE (content_id, season_number)
);

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
);

-- ============================================================
-- ОТЗЫВЫ (REVIEWS)
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_id  INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    rating      SMALLINT CHECK (rating BETWEEN 1 AND 10),
    body        TEXT,
    is_spoiler  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ЛАЙКИ НА ОТЗЫВЫ
-- ============================================================

CREATE TABLE IF NOT EXISTS review_likes (
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_id  INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, review_id)
);

-- ============================================================
-- КОММЕНТАРИИ К ОТЗЫВАМ
-- ============================================================

CREATE TABLE IF NOT EXISTS review_comments (
    id        SERIAL PRIMARY KEY,
    review_id INTEGER NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id INTEGER REFERENCES review_comments(id) ON DELETE CASCADE,  -- для вложенных ответов
    body      TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ИНДЕКСЫ
-- ============================================================

-- Users & tokens
CREATE INDEX IF NOT EXISTS idx_users_email             ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username          ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role              ON users(role);

CREATE INDEX IF NOT EXISTS idx_tokens_user_id          ON tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_token            ON tokens(token);
CREATE INDEX IF NOT EXISTS idx_tokens_refresh_token    ON tokens(refresh_token);

-- Persons
CREATE INDEX IF NOT EXISTS idx_persons_full_name       ON persons(full_name);

-- Content
CREATE INDEX IF NOT EXISTS idx_content_type            ON content(type);
CREATE INDEX IF NOT EXISTS idx_content_title           ON content(title);
CREATE INDEX IF NOT EXISTS idx_content_release_date    ON content(release_date);

CREATE INDEX IF NOT EXISTS idx_alt_titles_content_id   ON content_alt_titles(content_id);

CREATE INDEX IF NOT EXISTS idx_cast_content_id         ON content_cast(content_id);
CREATE INDEX IF NOT EXISTS idx_cast_person_id          ON content_cast(person_id);

CREATE INDEX IF NOT EXISTS idx_staff_content_id        ON content_staff(content_id);
CREATE INDEX IF NOT EXISTS idx_staff_person_id         ON content_staff(person_id);

-- Seasons & Episodes
CREATE INDEX IF NOT EXISTS idx_seasons_content_id      ON seasons(content_id);
CREATE INDEX IF NOT EXISTS idx_episodes_season_id      ON episodes(season_id);

CREATE INDEX IF NOT EXISTS idx_reviews_user_id      ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_content_id   ON reviews(content_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating       ON reviews(rating);

CREATE INDEX IF NOT EXISTS idx_review_likes_review_id  ON review_likes(review_id);

CREATE INDEX IF NOT EXISTS idx_review_comments_review_id  ON review_comments(review_id);
CREATE INDEX IF NOT EXISTS idx_review_comments_user_id    ON review_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_review_comments_parent_id  ON review_comments(parent_id);