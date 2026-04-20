from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import StreamingResponse
import yt_dlp
import requests
import logging
import time

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

app = FastAPI(title="Singify yt-dlp Audio Service")

# Cache resolved URLs to avoid re-searching YouTube on every play.
# YouTube signed URLs typically expire after ~6 hours; we use 5h to be safe.
_url_cache: dict[str, tuple[str, float]] = {}
_CACHE_TTL = 5 * 3600


def _cache_key(artist: str, title: str) -> str:
    return f"{artist.lower()}::{title.lower()}"


def _pick_audio_format(info: dict) -> str:
    """Return the URL of the best audio format from an already-extracted info dict."""
    formats = info.get("formats", [])
    audio_only = sorted(
        [f for f in formats if f.get("url") and f.get("vcodec") == "none" and f.get("acodec", "none") != "none"],
        key=lambda f: f.get("abr") or 0,
        reverse=True,
    )
    with_audio = [f for f in formats if f.get("url") and f.get("acodec", "none") != "none"]
    candidates = audio_only or with_audio
    if candidates:
        fmt = candidates[0]
        log.info("Selected format: %s ext=%s abr=%s", fmt.get("format_id"), fmt.get("ext"), fmt.get("abr"))
        return fmt["url"]
    if info.get("url"):
        return info["url"]
    raise ValueError("No audio stream found")


def search_audio_url(artist: str, title: str) -> str:
    """Search YouTube for a karaoke/instrumental version and return its audio stream URL."""
    key = _cache_key(artist, title)
    cached = _url_cache.get(key)
    if cached:
        url, expires_at = cached
        if time.time() < expires_at:
            log.info("Cache hit for %s - %s", artist, title)
            return url

    query = f"{artist} {title} karaoke instrumental"
    log.info("Searching: %s", query)
    ydl_opts = {
        "quiet": True,
        "no_warnings": True,
        "extract_flat": False,
        "default_search": "ytsearch1",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(query, download=False)
        if "entries" in info:
            info = info["entries"][0]

    url = _pick_audio_format(info)
    _url_cache[key] = (url, time.time() + _CACHE_TTL)
    return url


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/stream")
def stream_audio(artist: str = Query(...), title: str = Query(...)):
    """
    Proxy-stream audio for a given artist + title.
    The frontend audio element hits this endpoint directly.
    """
    log.info("Stream request: artist=%s title=%s", artist, title)
    try:
        audio_url = search_audio_url(artist, title)
    except Exception as e:
        log.error("Failed to find audio: %s", e)
        raise HTTPException(status_code=404, detail=f"Audio not found: {e}")

    # Proxy the stream so CORS is handled server-side
    upstream = requests.get(audio_url, stream=True, timeout=10)

    def generate():
        for chunk in upstream.iter_content(chunk_size=8192):
            if chunk:
                yield chunk

    content_type = upstream.headers.get("Content-Type", "audio/mpeg")
    return StreamingResponse(generate(), media_type=content_type)


@app.get("/url")
def get_audio_url(artist: str = Query(...), title: str = Query(...)):
    """
    Return the raw YouTube audio stream URL (for backend use).
    """
    log.info("URL request: artist=%s title=%s", artist, title)
    try:
        audio_url = search_audio_url(artist, title)
        return {"url": audio_url}
    except Exception as e:
        log.error("Failed to find audio: %s", e)
        raise HTTPException(status_code=404, detail=f"Audio not found: {e}")
