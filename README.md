# InterviewPair

Find a partner for a mock technical interview by topic, level and time.

Status: early scaffold. The server builds, the matching rule is implemented and tested, and the web app talks to the server. Auth, availability and live matching are next. See [docs/DESIGN.md](docs/DESIGN.md).

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

Open the forwarded port 5173. The page shows whether the server is reachable.

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
