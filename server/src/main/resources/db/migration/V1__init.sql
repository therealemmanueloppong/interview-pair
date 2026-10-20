CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE TABLE availability (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic      VARCHAR(40)  NOT NULL,
    level      VARCHAR(20)  NOT NULL,
    starts_at  TIMESTAMP    NOT NULL,
    ends_at    TIMESTAMP    NOT NULL,
    CHECK (ends_at > starts_at)
);

CREATE INDEX idx_availability_topic_time ON availability (topic, starts_at, ends_at);

CREATE TABLE matches (
    id           UUID PRIMARY KEY,
    user_a       UUID         NOT NULL REFERENCES users (id),
    user_b       UUID         NOT NULL REFERENCES users (id),
    topic        VARCHAR(40)  NOT NULL,
    starts_at    TIMESTAMP    NOT NULL,
    meeting_link VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL,
    CHECK (user_a <> user_b)
);
