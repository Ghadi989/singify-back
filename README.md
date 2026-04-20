# Singify — Karaoke Platform

> **Backend API** built with Spring Boot 3.4.4 · Java 21 · PostgreSQL 16 · Apache Kafka · Docker

---

## Quick Start

```bash
# 1. Place your GCS credentials file at the project root
cp /path/to/gcs-credentials.json ./gcs-credentials.json

# 2. Create your .env file
cp .env.example .env

# 3. Start the full stack
docker compose up --build
```

Once started, the backend logs will print:

```
╔══════════════════════════════════════════════════╗
║           SINGIFY  —  App is ready!              ║
╠══════════════════════════════════════════════════╣
║  Frontend     →  http://localhost:3000           ║
║  Backend API  →  http://localhost:8080/api/songs ║
║  Kafka UI     →  http://localhost:8090           ║
╚══════════════════════════════════════════════════╝
```

### Service URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Kafka UI | http://localhost:8090 |
| PostgreSQL | localhost:5432 |

---

## Prerequisites

| Tool | Minimum Version |
|---|---|
| Docker Desktop | 4.x |

No local Java or Node required — everything runs inside Docker.

---

## Local Development (without Docker)

### Backend

```bash
# Requires local PostgreSQL on localhost:5432 and Kafka on localhost:9092
./mvnw spring-boot:run
# Uses application-dev.yml by default
```

### Frontend

```bash
cd ../singify-front
npm install
npm run dev
```

---

## Tests

```bash
./mvnw test
```

---

## Architecture

```
┌─────────────────┐        HTTP / REST        ┌──────────────────────┐
│                 │ ────────────────────────► │                      │
│    Frontend     │                           │    Backend (8080)    │
│   (Vue 3)       │ ◄──────────────────────── │   Spring Boot 3.4.4  │
│    :3000        │        JSON               │                      │
└─────────────────┘                           └──────────┬───────────┘
                                                         │
                                    ┌────────────────────┼──────────────────┐
                                    │                    │                  │
                                    ▼                    ▼                  ▼
                           ┌─────────────┐     ┌──────────────┐  ┌──────────────────┐
                           │ PostgreSQL  │     │ Apache Kafka │  │  Google Cloud    │
                           │    :5432    │     │    :9092     │  │  Storage (GCS)   │
                           │            │     │              │  │                  │
                           │  songs     │     │  Topics:     │  │  MP3 audio files │
                           │  users     │     │  song.played │  └──────────────────┘
                           └─────────────┘     │  song.liked  │
                                               └──────────────┘
```

## Kafka Topics

| Topic | Triggered by | Purpose |
|---|---|---|
| `song.played` | `GET /api/songs/{id}` | User opened a song |
| `song.liked` | `POST /api/library/like/{id}` | User liked a song |

---

## CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`) runs on every push to `main`/`develop`:

1. **Build** — compiles the JAR
2. **Test** — runs tests against live PostgreSQL + Kafka containers
3. **Coverage report** — generates JaCoCo HTML report (uploaded as artifact)
4. **Docker build** — verifies the Docker image builds (on `main` only)

---

## Security

JWT-based authentication. Tokens expire after 24 hours.

### Public endpoints

| Method | Path |
|---|---|
| POST | `/api/auth/register` |
| POST | `/api/auth/login` |
| GET | `/api/songs/**` |
| GET | `/actuator/health` |

### Protected endpoints

All `/api/library/**` endpoints require a valid JWT in the `Authorization: Bearer <token>` header.
