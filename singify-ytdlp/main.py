from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import StreamingResponse
import yt_dlp
import requests
import logging

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

app = FastAPI(title="Singify yt-dlp Audio Service")


def search_audio_url(artist: str, title: str) -> str:
    """Search YouTube and return the best audio stream URL."""
    query = f"{artist} {title} official audio"
    ydl_opts = {
        "format": "bestaudio/best",
        "quiet": True,
        "no_warnings": True,
        "extract_flat": False,
        "default_search": "ytsearch1",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(query, download=False)
        if "entries" in info:
            info = info["entries"][0]
        # Find best audio format URL
        for fmt in reversed(info.get("formats", [])):
            if fmt.get("acodec") != "none" and fmt.get("url"):
                return fmt["url"]
        raise ValueError("No audio stream found")


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
