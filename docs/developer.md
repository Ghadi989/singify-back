# Singify — Developer Setup

## Prerequisites

| Tool | Version |
|---|---|
| Docker Desktop | 24+ (with Compose v2) |
| Git | any |
| Free disk space | ≥ 15 GB (ML model + Docker layers) |
| RAM | ≥ 8 GB recommended |

---

## 1 — Clone

```bash
git clone <repo-url> singify-front
git clone <repo-url> singify-back
```

Both repos must sit side-by-side because the backend `docker-compose.yml` references `../singify-front` for the frontend build context.

---

## 2 — Environment file

Copy the example and fill in the blanks:

```bash
cp singify-back/.env.example singify-back/.env
```

Required values in `.env`:

```env
SPOTIFY_CLIENT_ID=<from developer.spotify.com>
SPOTIFY_CLIENT_SECRET=<from developer.spotify.com>
GCS_BUCKET_NAME=<your GCS bucket>
GCS_CREDENTIALS_FILE=./singify-491509-403a0763c041.json   # path relative to singify-back/
GCS_CREDENTIALS_PATH=/app/gcs-credentials.json             # leave as-is
JWT_SECRET=<any long random string>
```

Place the GCS service-account JSON key inside `singify-back/` so the Docker volume mount can find it.

---

## 3 — Start everything

```bash
cd singify-back
docker compose up --build
```

First build takes **5–10 minutes** (downloads ML model, Maven dependencies, Node modules). Subsequent builds are fast thanks to layer caching.

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| yt-dlp service | http://localhost:8000 |
| Kafka UI | http://localhost:8090 |

---

## 4 — Rebuilding a single service

```bash
# Only rebuild the yt-dlp container (e.g. after changing main.py)
docker compose build ytdlp && docker compose up -d --force-recreate ytdlp
```

---

## Architecture overview

```
Browser
  └─ singify-front (Vue 3 + Vite, served by nginx)
       └─ singify-back (Spring Boot 3)
            ├─ PostgreSQL  — song metadata, users, likes
            ├─ Kafka       — song-played events
            ├─ GCS         — karaoke MP3 files (permanent storage)
            └─ singify-ytdlp (FastAPI)
                 ├─ yt-dlp    — YouTube audio download
                 └─ audio-separator (ONNX) — vocal removal
```

**Karaoke pipeline:** frontend triggers `POST /api/audio/process` → Spring Boot calls yt-dlp `/process` asynchronously → yt-dlp downloads audio + removes vocals → Spring Boot uploads instrumental to GCS → saves song to DB → frontend polls `/api/audio/status` every 5 s until ready.

---

## Useful commands

```bash
# Tail all logs
docker compose logs -f

# Reset the database (drops all rows, keeps schema)
docker exec -it singify-postgres psql -U singify -d singify \
  -c "TRUNCATE TABLE library CASCADE; TRUNCATE TABLE songs CASCADE;"

# Clear GCS bucket via admin endpoint
curl -X DELETE http://localhost:8080/api/admin/gcs/files

# Force-clear the yt-dlp URL cache
curl -X POST http://localhost:8000/cache/clear
```
