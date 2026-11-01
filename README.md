# InterviewPair

Find a partner for a mock technical interview by topic, level and time.

Status: early build. Register, login and a protected profile endpoint work with signed JWT tokens, and the web app has a light/dark practice dashboard (weekly goal, focus timer, curated prompts, a reflection notebook that stays in your browser). **Mock interviews are working**: you schedule a topic, level and time, the server pairs you with another student, and you practice in a live room with a shared code editor where only the person answering can type, then switch roles. See [docs/MOCK_INTERVIEWS.md](docs/MOCK_INTERVIEWS.md), [the dashboard guide](docs/DASHBOARD.md) and [docs/DESIGN.md](docs/DESIGN.md).

## Stack

Java 21 and Spring Boot (REST, WebSocket, Security), PostgreSQL with Flyway migrations, React with TypeScript, Vite and Tailwind, JUnit 5, Vitest, GitHub Actions.

## Run it

You need Java 21, Node 22 and Docker. The repo includes a dev container, so opening it in GitHub Codespaces sets everything up.

```bash
docker compose up -d          # PostgreSQL
cd server && ./mvnw spring-boot:run
```

In a second terminal:

```bash
cd web && npm install && npm run dev
```

Open the forwarded port 5173. You can create an account, log in, and log out. To try a mock interview on your own, open the app in two different browsers (or one normal and one private window), create two accounts, and schedule the same topic, level and time from both. The second request pairs you, and the two rooms update live. In a Codespace the server gets a development-only `JWT_SECRET` from the dev container. Anywhere else, set `JWT_SECRET` to a random string of at least 32 characters before starting the server.

## Test it

```bash
cd server && ./mvnw test
cd web && npm test
```

Server tests use an in-memory H2 database in PostgreSQL mode. A Testcontainers suite against real PostgreSQL is planned.

## Deploy

One Docker image serves the whole app (Spring Boot serves the built React pages). See [docs/DEPLOY.md](docs/DEPLOY.md) for Neon, Render and a local Docker test.

## Layout

- `server/` Spring Boot API
- `web/` React app
- `docs/` design notes and deploy guide
- `Dockerfile`, `render.yaml` deploy setup
