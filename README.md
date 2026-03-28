# Singify — Karaoke Platform

> **Backend API** built with Spring Boot 3.2.5 · Java 21 · PostgreSQL 16 · Apache Kafka · Docker

---

## Prerequisites

| Tool | Minimum Version |
|---|---|
| Docker | 24.x |
| Docker Compose | 2.20 |
| Java | 21 (Temurin recommended) |
| Node | 20.x |

---

## Quick Start

```bash
# 1. Clone both repos side by side
git clone https://github.com/your-org/singify-back.git
git clone https://github.com/your-org/singify-front.git

# 2. Start the full stack
cd singify-back

# If your Docker installation supports the newer CLI plugin (Docker Desktop ≥ 4.x):
docker compose up --build

# If the above is not found (older standalone docker-compose binary):
docker-compose up --build
```

> **WSL users:** if `docker` is not found when running commands in PowerShell or Command Prompt, open a **WSL terminal** (e.g. Ubuntu) and run the commands from there instead. Docker Desktop exposes its daemon to WSL 2 automatically when the WSL integration is enabled in Docker Desktop settings.

### Service URLs

| Service | URL |
|---|---|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Frontend | http://localhost:3000 |
| Kafka UI | http://localhost:8090 |
| PostgreSQL | localhost:5432 |

---

## Local Development

### Backend

```bash
# Run with Maven wrapper (requires a local PostgreSQL and Kafka, or override env vars)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/singify
export SPRING_DATASOURCE_USERNAME=singify
export SPRING_DATASOURCE_PASSWORD=singify
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

./mvnw spring-boot:run
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
# Run all tests
./mvnw test

# Run tests and generate JaCoCo coverage report
./mvnw test jacoco:report

# Open the HTML report in your browser
open target/site/jacoco/index.html          # macOS
xdg-open target/site/jacoco/index.html     # Linux
start target/site/jacoco/index.html        # Windows
```

The build enforces a minimum **70% line coverage**. The pipeline fails if this threshold is not met (`./mvnw jacoco:check`).

---

## Architecture

```
┌─────────────────┐        HTTP / REST        ┌──────────────────────┐
│                 │ ────────────────────────► │                      │
│    Frontend     │                           │    Backend (8080)    │
│   (Next.js)     │ ◄──────────────────────── │   Spring Boot 3.2.5  │
│    :3000        │        JSON               │                      │
└─────────────────┘                           └──────────┬───────────┘
                                                         │
                                    ┌────────────────────┼────────────────────┐
                                    │                    │                    │
                                    ▼                    ▼                    ▼
                           ┌─────────────┐     ┌─────────────────┐  ┌────────────────┐
                           │ PostgreSQL  │     │  Apache Kafka   │  │   Actuator /   │
                           │    :5432    │     │     :9092       │  │  Swagger UI    │
                           │            │     │                 │  │    :8080       │
                           │  songs     │     │  Topics:        │  └────────────────┘
                           │  lyrics    │     │  ├ song-search  │
                           └─────────────┘     │  ├ karaoke-    │
                                               │  │  session    │
                                               │  └ lyrics-     │
                                               │    ready       │
                                               └─────────────────┘
```

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `song-search-events` | `SongEventProducer` | `SongEventConsumer` | Published whenever a user searches for a song by title or artist |
| `karaoke-session-events` | `SongEventProducer` | `SongEventConsumer` | Published when a karaoke session is started for a song |
| `lyrics-ready-events` | `LyricsService` | `SongEventConsumer` | Published when lyrics are saved or updated for a song |

All topics are created at startup via `KafkaTopicConfig` with **3 partitions** and **1 replica**.

---

## CI/CD

The GitHub Actions pipeline (`.github/workflows/ci.yml`) runs on every push and pull request to `main` and `develop` and executes four steps in order:

1. **Build** — compiles the project and packages the JAR (`./mvnw clean package -DskipTests`)
2. **Test** — runs the full test suite against live PostgreSQL and Kafka service containers (`./mvnw test`)
3. **Coverage report** — generates the JaCoCo HTML/XML report and uploads it as a workflow artifact (`./mvnw jacoco:report`)
4. **Coverage check** — fails the build if line coverage drops below 70% (`./mvnw jacoco:check`)

A second job (`docker-build`) runs only on pushes to `main`, depends on the test job passing, and verifies the Docker image builds successfully (`push: false`).

---

## Security

Spring Security is configured with HTTP Basic authentication.

### Public endpoints (no credentials required)

| Method | Path |
|---|---|
| GET | `/api/songs/**` |
| GET | `/api/lyrics/**` |
| GET | `/actuator/health` |
| GET | `/swagger-ui.html`, `/swagger-ui/**`, `/api-docs/**` |

### Protected endpoints (credentials required)

All `POST`, `PUT`, and `DELETE` requests require authentication.

**Default credentials** (in-memory, development only):

| Username | Password | Role |
|---|---|---|
| `admin` | `admin` | `ADMIN` |

> **Note:** The in-memory user is intended for local development and testing only. Replace it with a proper identity provider before deploying to production.
