-- A person asks for a mock interview in a two hour slot. When two people ask for the same
-- topic, level and slot, the server creates one practice session for them.
CREATE TABLE interview_requests (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic      VARCHAR(40) NOT NULL,
    level      VARCHAR(20) NOT NULL,
    slot_start TIMESTAMP   NOT NULL,
    status     VARCHAR(20) NOT NULL,
    session_id UUID,
    created_at TIMESTAMP   NOT NULL
);

CREATE INDEX idx_interview_requests_pool ON interview_requests (status, topic, level, slot_start);
CREATE INDEX idx_interview_requests_user ON interview_requests (user_id, status);

-- One session has two rounds. In round one user_a or user_b answers question_one while the other
-- interviews. The roles swap for round two and question_two. Only the interviewee can change code.
CREATE TABLE practice_sessions (
    id             UUID PRIMARY KEY,
    topic          VARCHAR(40) NOT NULL,
    level          VARCHAR(20) NOT NULL,
    slot_start     TIMESTAMP   NOT NULL,
    user_a         UUID        NOT NULL REFERENCES users (id),
    user_b         UUID        NOT NULL REFERENCES users (id),
    round          INT         NOT NULL CHECK (round IN (1, 2)),
    question_one   VARCHAR(60) NOT NULL,
    question_two   VARCHAR(60) NOT NULL,
    interviewee_id UUID        NOT NULL REFERENCES users (id),
    code           TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP   NOT NULL,
    CHECK (user_a <> user_b)
);

CREATE INDEX idx_practice_sessions_user_a ON practice_sessions (user_a, slot_start);
CREATE INDEX idx_practice_sessions_user_b ON practice_sessions (user_b, slot_start);

CREATE TABLE session_feedback (
    id         UUID PRIMARY KEY,
    session_id UUID          NOT NULL REFERENCES practice_sessions (id) ON DELETE CASCADE,
    from_user  UUID          NOT NULL REFERENCES users (id),
    to_user    UUID          NOT NULL REFERENCES users (id),
    rating     INT           NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP     NOT NULL,
    UNIQUE (session_id, from_user)
);
