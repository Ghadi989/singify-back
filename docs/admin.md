# Singify — Admin & Ops Guide

## GCS Setup

### 1. Create the bucket

In [Google Cloud Console](https://console.cloud.google.com) → Cloud Storage → Create bucket.  
Recommended settings: region `europe-west1`, standard storage class, no public access.

### 2. Create a service account

IAM & Admin → Service Accounts → Create.  
Name: `storage-object-admin` (or any name).  
Grant role: **Storage Object Admin** on the bucket (not project-wide).

### 3. Generate a key

Service account → Keys → Add key → JSON.  
Place the downloaded file at `singify-back/<filename>.json` and point `.env` to it:

```env
GCS_CREDENTIALS_FILE=./<filename>.json
```

---

## Admin API Endpoints

All endpoints are available at `http://localhost:8080/api/admin/gcs/`.  
No auth required in dev — add a guard before any public deployment.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/gcs/files` | List all files stored in GCS |
| `GET` | `/api/admin/gcs/status` | Check GCS connectivity and bucket name |
| `GET` | `/api/admin/gcs/files/url?key=…` | Get a signed URL for a specific key |
| `DELETE` | `/api/admin/gcs/files` | Delete **all** files in the bucket |
| `DELETE` | `/api/admin/gcs/files/by-song?artist=…&title=…` | Delete one song's karaoke file |

---

## Monitoring the Pipeline

Watch yt-dlp logs in real time:

```bash
docker compose logs -f ytdlp
```

A successful run looks like:

```
🎵 KARAOKE PIPELINE START: Adele - Skyfall
[1/3] DOWNLOAD  ✅ Done in 8.9s — 6.62 MB
[2/3] SEPARATE  Removing vocals (UVR-MDX-NET-Inst_HQ_3.onnx, ONNX/CPU)...
[2/3] SEPARATE  ✅ Done in 87.4s — 5.10 MB
[3/3] RESPONSE  Sending instrumental to backend for GCS upload...
✅ PIPELINE COMPLETE: Adele - Skyfall  (total 98.2s)
```

Backend confirmation:

```bash
docker compose logs -f backend | grep BACKEND
```

```
▶ [BACKEND] Requesting instrumental from yt-dlp service: Adele - Skyfall
▶ [BACKEND] Received 5222 KB — uploading to GCS: audio/adele-skyfall.mp3
✅ [BACKEND] GCS upload complete — karaoke ready: audio/adele-skyfall.mp3
✅ [BACKEND] Song saved to DB: Adele - Skyfall
```

---

## Common Problems

| Symptom | Cause | Fix |
|---|---|---|
| Pipeline fails with `403 Forbidden` | Service account missing **Storage Object Admin** on bucket | Grant the role in GCS Console → bucket → Permissions |
| GCS disabled on startup | Credentials file not found by Docker volume mount | Check `GCS_CREDENTIALS_FILE` in `.env` points to an existing file inside `singify-back/` |
| Normal song plays instead of karaoke | Old normal-version file was cached in GCS | `DELETE /api/admin/gcs/files/by-song?artist=…&title=…` then re-trigger |
| Build fails with `No space left on device` | Docker layer cache full | `docker system prune -af --volumes` then rebuild |
| `torchvision::nms` error in yt-dlp | `torchvision` not installed in container | Rebuild yt-dlp image: `docker compose build ytdlp` |

---

## Disk & Memory Requirements

| Resource | Minimum |
|---|---|
| Docker image size (ytdlp) | ~3 GB |
| RAM during vocal separation | ~2 GB |
| GCS storage per song | ~5 MB per karaoke MP3 |
| Free disk for builds | 15 GB |
