# InterviewPair design doc

Status: draft v1, to be revised as the build teaches us things.

## Problem

Students who practice technical interviews alone miss the part that matters most: explaining their thinking to another person under time pressure. Finding a reliable partner at the right level, on the right topic, at a time that works for both people is the hard part. Scheduling by chat is slow and most requests never turn into a session.

## Users

CS students preparing for internship interviews, starting with the ColorStack community.

## Core flow

1. Sign up with an email and password.
2. Post availability: a topic (for example graphs or dynamic programming), a level (easy, medium, hard) and a time window.
3. The server finds a partner with the same topic and level whose window overlaps by at least 45 minutes, and creates a match.
4. Both people get a notification and a meeting link. They swap roles halfway through.
5. After the session each person leaves a short rating, which feeds future matching.

## Matching rule (v1)

Same topic, same level, a different user, and at least 45 minutes of overlap. If several people qualify, pick the longest overlap. This is implemented and unit tested in `MatchingService`. Later versions may weigh ratings and no-show history.

## Data model

- `users`: id, email, password hash, display name, created at.
- `availability`: id, user, topic, level, starts at, ends at.
- `matches`: id, the two users, topic, starts at, meeting link, created at.

The schema lives in Flyway migrations under `server/src/main/resources/db/migration`.

## API (v1)

- `POST /api/auth/register`, `POST /api/auth/login` (returns a JWT)
- `POST /api/availability`, `GET /api/availability/mine`, `DELETE /api/availability/{id}`
- `GET /api/matches/mine`
- WebSocket (STOMP) topic `/user/queue/matches` pushes a new match to the people in it
- `GET /api/health` (public)

## Architecture

React and TypeScript single-page app, a Spring Boot REST and WebSocket API, and PostgreSQL. Video is out of scope: a match gets a generated meeting link from an existing provider.

## Out of scope for v1

Custom video, a code editor shared between partners, payments, mobile apps, and anything involving recorded sessions.

## Milestones

1. Auth with JWT (register, login, protected routes).
2. Availability CRUD with validation.
3. Matching job that creates matches and pushes them over WebSocket.
4. Web UI for all of the above.
5. Integration tests on a real Postgres with Testcontainers, then deploy.
6. Pilot with ColorStack members and record real numbers.
