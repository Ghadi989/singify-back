# Singify — Karaoke Streaming Platform

A fullstack karaoke application: search songs via Spotify, stream audio, get synchronized lyrics, and build a personal library.

```
singify/
├── singify-back/   Spring Boot 3.4.4 · Java 21 · PostgreSQL · Kafka  ← this repo
└── singify-front/  Vue 3 · TypeScript · Vite
```

---

## Deployment

### Prerequisites

| Tool | Minimum version |
|---|---|
| Docker Desktop | 4.x |

No local Java or Node required — everything runs inside Docker.

### Steps

```bash
# 1. Copy the environment file and fill in values (see Environment Variables below)
cp .env.example .env

# 2. Place your Google Cloud Storage credentials file at the project root
cp /path/to/gcs-credentials.json ./gcs-credentials.json

# 3. Start the full stack
docker compose up --build
```

The backend prints a banner when ready:

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

### Environment Variables

All variables are set in `.env` (copy from `.env.example`).

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | Yes | Secret key used to sign JWT tokens — use a long random string in production |
| `GCS_BUCKET_NAME` | Yes | Google Cloud Storage bucket name for karaoke audio files |
| `GCS_CREDENTIALS_FILE` | Local dev | Path to GCS service account JSON for local development |
| `GCS_CREDENTIALS_PATH` | Docker | Path to GCS credentials inside the container (`/app/gcs-credentials.json`) |
| `CORS_ALLOWED_ORIGINS` | Yes | Comma-separated list of allowed frontend origins — e.g. `https://singify.example.com` |
| `SPOTIFY_CLIENT_ID` | No | Spotify API client ID — enables Spotify search; falls back to local DB if absent |
| `SPOTIFY_CLIENT_SECRET` | No | Spotify API client secret |

> Spotify credentials are optional. Without them, search queries run against the local PostgreSQL database.

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
# Coverage report generated at target/site/jacoco/index.html
```

---

## API Endpoints

Base URL: `http://localhost:8080`

Authentication uses JWT. Protected endpoints require the header:
```
Authorization: Bearer <token>
```

### Authentication — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create a new user account |
| POST | `/api/auth/login` | Public | Login and receive a JWT token |

### Songs — `/api/songs`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/songs` | Optional | List all songs (liked status included if authenticated) |
| GET | `/api/songs/recommended` | Optional | Recommended songs |
| GET | `/api/songs/recent` | Optional | Recently added songs |
| GET | `/api/songs/search?q={query}` | Optional | Search songs (Spotify if configured, local DB otherwise) |
| GET | `/api/songs/{id}` | Optional | Get a song by ID — fires `song.played` Kafka event |
| GET | `/api/songs/{id}/lyrics` | Optional | Get synchronized lyrics for a song by ID |
| GET | `/api/songs/lyrics?artist={artist}&title={title}` | Public | Direct lyrics lookup for Spotify results not stored locally |

### Library — `/api/library`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/library` | Required | Get the authenticated user's liked songs |
| POST | `/api/library/{songId}` | Required | Like a song — fires `song.liked` Kafka event |
| DELETE | `/api/library/{songId}` | Required | Unlike a song |

### Audio — `/api/audio`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/audio/status?artist={artist}&title={title}` | Public | Check karaoke processing status (`pending` / `processing` / `ready`) — returns signed GCS URL when ready |
| POST | `/api/audio/process?spotifyId={id}&artist={artist}&title={title}` | Public | Trigger async karaoke processing pipeline |
| GET | `/api/audio/stream?artist={artist}&title={title}` | Public | Stream audio (proxies YouTube, or redirects to GCS if karaoke version is ready) |

### Admin — `/api/admin/gcs`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/gcs/files` | Public | List all cached karaoke files in GCS (name, size, timestamp) |
| DELETE | `/api/admin/gcs/files?key={key}` | Public | Delete a cached file by its GCS key |
| DELETE | `/api/admin/gcs/files/by-song?artist={artist}&title={title}` | Public | Delete a cached file by artist and title |
| GET | `/api/admin/gcs/files/url?key={key}` | Public | Get a fresh signed URL for a cached file (valid 6 h) |
| GET | `/api/admin/gcs/status?artist={artist}&title={title}` | Public | Check in-memory processing status for a song |

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
                           │  songs     │     │  song.played │  │  Karaoke MP3s    │
                           │  users     │     │  song.liked  │  └──────────────────┘
                           └─────────────┘     └──────────────┘
```

## Kafka Topics

| Topic | Triggered by | Purpose |
|---|---|---|
| `song.played` | `GET /api/songs/{id}` | User opened a song |
| `song.liked` | `POST /api/library/{songId}` | User liked a song |

---

## CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`):

- **Feature branches / PRs** → build + test + coverage report
- **`main` / `pre-prod`** → build + test + coverage report + Docker image build

| Step | Description |
|---|---|
| Build | Compiles the JAR (`mvn package`) |
| Test | Runs unit and integration tests against H2 in-memory DB |
| Coverage | Generates JaCoCo HTML report (uploaded as CI artifact) |
| Docker build | Verifies the Docker image builds (long-lived branches only) |

---

## Security

JWT-based authentication. Tokens expire after 24 hours. Pass the token in every protected request:

```
Authorization: Bearer <your-token>
```

Tokens are obtained from `POST /api/auth/login`.
