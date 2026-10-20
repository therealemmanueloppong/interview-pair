# Deploying InterviewPair

One Docker image serves the whole app: the Spring Boot server also serves the built React pages, so there is a single URL. The database is a separate free PostgreSQL on Neon. Free-tier limits change, so check each site's pricing page.

## 1. Database on Neon

1. Create a project at neon.tech and copy the connection details: host, database name, user and password.
2. Build the JDBC URL for the server. If Neon shows `postgresql://USER:PASSWORD@HOST/DB?sslmode=require`, then the three settings are:
   - `DB_URL` = `jdbc:postgresql://HOST/DB?sslmode=require`
   - `DB_USER` = `USER`
   - `DB_PASSWORD` = `PASSWORD`

The server creates its tables itself on first start (Flyway migrations).

## 2. Web service on Render

1. In Render choose New, then Blueprint, and select this repository. Render reads `render.yaml`.
2. Fill in the three settings above when asked. Do not commit them to GitHub.
3. Wait for the first build. Then open the Render URL.

Checks: `/api/health` returns `{"status":"ok",...}`, and `/` shows the app saying "Server online".

## Notes

- The free web service sleeps when idle, so the first request after a pause can take about a minute.
- Every push to `main` redeploys automatically.
- Secrets such as the future JWT signing key are added the same way, as environment variables in Render.

## Test the image locally

```bash
docker build -t interview-pair .
docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:h2:mem:local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE' -e DB_USER=sa -e DB_PASSWORD= \
  interview-pair
```

Then open http://localhost:8080. This uses a throwaway in-memory database.
