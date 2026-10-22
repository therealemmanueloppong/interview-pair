# InterviewPair

Find a partner for a mock technical interview by topic, level and time.

Status: early build. Register, login and a protected profile endpoint work with signed JWT tokens, the matching rule is implemented and tested, and the web app includes a light/dark practice dashboard. After login, users can set a weekly goal, practice with curated prompts and a focus timer, and keep a personal reflection notebook. Practice records are browser-local and scoped to the signed-in account; they are not synced to the server. Availability and live partner matching are next. See [the dashboard guide](docs/DASHBOARD.md). See [docs/DESIGN.md](docs/DESIGN.md).

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

Open the forwarded port 5173. You can create an account, log in, and log out. In a Codespace the server gets a development-only `JWT_SECRET` from the dev container. Anywhere else, set `JWT_SECRET` to a random string of at least 32 characters before starting the server.

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
