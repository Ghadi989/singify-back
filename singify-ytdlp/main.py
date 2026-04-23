from fastapi import FastAPI, HTTPException, Query, BackgroundTasks
from fastapi.responses import StreamingResponse, FileResponse
from audio_separator.separator import Separator
import yt_dlp
import requests
import subprocess
import logging
import time
import tempfile
import os
import shutil

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

app = FastAPI(title="Singify yt-dlp Audio Service")

# The MDX-Net ONNX model — fast on CPU (~60-90s for a 4-min song), good quality
MODEL_NAME = "UVR-MDX-NET-Inst_HQ_3.onnx"

# Cache resolved URLs to avoid re-searching YouTube on every play.
# YouTube signed URLs typically expire after ~6 hours; we use 5h to be safe.
_url_cache: dict[str, tuple[str, float]] = {}
_CACHE_TTL = 5 * 3600
_COOKIES_FILE = "/tmp/yt-cookies.txt"

# Piped instances in priority order — bypasses YouTube bot detection entirely
_PIPED_INSTANCES = [
    "https://pipedapi.kavin.rocks",
    "https://piped-api.garudalinux.org",
    "https://api.piped.yt",
]


def _piped_get_audio(artist: str, title: str) -> tuple[str, str]:
    """Search Piped for artist+title, return (proxied_stream_url, mime_type).
    Piped proxies YouTube CDN so Railway never hits YouTube directly."""
    query = f"{artist} {title}"
    last_err = None
    for base in _PIPED_INSTANCES:
        try:
            search = requests.get(
                f"{base}/search", params={"q": query, "filter": "all"}, timeout=10
            ).json()
            items = [i for i in search.get("items", []) if i.get("type") == "stream"]
            if not items:
                continue
            video_id = items[0]["url"].split("v=")[-1].split("&")[0]
            streams = requests.get(f"{base}/streams/{video_id}", timeout=10).json()
            audio = sorted(
                streams.get("audioStreams", []),
                key=lambda s: s.get("bitrate", 0),
                reverse=True,
            )
            if not audio:
                continue
            url = audio[0]["url"]
            mime = audio[0].get("mimeType", "audio/webm")
            log.info("Piped hit: %s — bitrate=%s", base, audio[0].get("bitrate"))
            return url, mime
        except Exception as e:
            last_err = e
            log.warning("Piped instance %s failed: %s", base, e)
    raise ValueError(f"All Piped instances failed: {last_err}")


def _download_via_piped(artist: str, title: str, out_mp3: str) -> None:
    """Download audio from Piped proxy and convert to MP3 via ffmpeg."""
    stream_url, _ = _piped_get_audio(artist, title)
    temp = out_mp3.replace(".mp3", ".tmp.webm")
    log.info("Downloading from Piped proxy…")
    with requests.get(stream_url, stream=True, timeout=120) as r:
        r.raise_for_status()
        with open(temp, "wb") as f:
            for chunk in r.iter_content(chunk_size=65536):
                f.write(chunk)
    log.info("Converting to MP3 via ffmpeg…")
    result = subprocess.run(
        ["ffmpeg", "-y", "-i", temp, "-vn", "-ar", "44100", "-ac", "2", "-b:a", "192k", out_mp3],
        capture_output=True, text=True,
    )
    os.remove(temp)
    if result.returncode != 0:
        raise ValueError(f"ffmpeg failed: {result.stderr[-500:]}")


def _ydl_base_opts() -> dict:
    opts = {"quiet": True, "no_warnings": True, "socket_timeout": 30}
    if os.path.exists(_COOKIES_FILE):
        opts["cookiefile"] = _COOKIES_FILE
    return opts


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
    """Return a streamable audio URL for artist+title (Piped-proxied, cached)."""
    key = _cache_key(artist, title)
    cached = _url_cache.get(key)
    if cached:
        url, expires_at = cached
        if time.time() < expires_at:
            log.info("Cache hit for %s - %s", artist, title)
            return url

    log.info("Searching: %s", f"{artist} {title}")
    # Piped first — avoids YouTube PO token restrictions on datacenter IPs
    try:
        url, _ = _piped_get_audio(artist, title)
        _url_cache[key] = (url, time.time() + _CACHE_TTL)
        return url
    except Exception as e:
        log.warning("Piped failed, falling back to yt-dlp: %s", e)

    ydl_opts = {**_ydl_base_opts(), "extract_flat": False, "default_search": "ytsearch1"}
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(f"{artist} {title} lyrics", download=False)
        if "entries" in info:
            info = info["entries"][0]
    url = _pick_audio_format(info)
    _url_cache[key] = (url, time.time() + _CACHE_TTL)
    return url


def _download_mp3(artist: str, title: str, out_path: str) -> str:
    """Download audio for artist+title as MP3 at out_path. Piped-first, yt-dlp fallback."""
    # Piped: bypasses YouTube PO token restrictions
    try:
        _download_via_piped(artist, title, out_path)
        if os.path.exists(out_path):
            return out_path
    except Exception as e:
        log.warning("Piped download failed, falling back to yt-dlp: %s", e)

    # yt-dlp fallback
    query = f"ytsearch1:{artist} {title}"
    ydl_opts = {
        **_ydl_base_opts(),
        "format": "bestaudio/best",
        "outtmpl": out_path.replace(".mp3", ".%(ext)s"),
        "postprocessors": [{
            "key": "FFmpegExtractAudio",
            "preferredcodec": "mp3",
            "preferredquality": "192",
        }],
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        ydl.download([query])
    if not os.path.exists(out_path):
        raise ValueError(f"No MP3 produced for: {artist} - {title}")
    return out_path


def _find_instrumental(output_files: list[str], sep_dir: str) -> str:
    """Pick the instrumental (non-vocal) file from audio-separator output."""
    def full(f):
        return f if os.path.isabs(f) else os.path.join(sep_dir, f)

    # Prefer files explicitly labelled Instrumental
    for f in output_files:
        if "Instrumental" in os.path.basename(f) or "instrumental" in os.path.basename(f):
            return full(f)
    # Fall back: pick anything that isn't labelled Vocals
    for f in output_files:
        bn = os.path.basename(f)
        if "Vocal" not in bn and "vocal" not in bn:
            return full(f)
    # Last resort: first file
    if output_files:
        return full(output_files[0])
    raise ValueError("audio-separator produced no output files")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/cache/clear")
def clear_cache():
    _url_cache.clear()
    log.info("URL cache cleared")
    return {"cleared": True}


@app.get("/process")
def process_karaoke(
    spotify_id: str = Query(...),
    artist: str = Query(...),
    title: str = Query(...),
    background_tasks: BackgroundTasks = BackgroundTasks(),
):
    """
    Full karaoke pipeline:
      1. Download the original song from YouTube via yt-dlp
      2. Remove vocals with audio-separator (ONNX, UVR-MDX-NET — ~60-90s on CPU)
      3. Return the instrumental MP3 for the backend to upload to GCS
    """
    song_label = f"{artist} - {title}"
    log.info("=" * 60)
    log.info("🎵 KARAOKE PIPELINE START: %s", song_label)
    log.info("=" * 60)
    tmpdir = tempfile.mkdtemp()
    t_start = time.time()

    try:
        # ── STEP 1: Download original song ────────────────────────────
        log.info("[1/3] DOWNLOAD  Searching YouTube for: %s", song_label)
        input_mp3 = os.path.join(tmpdir, "audio.mp3")
        t1 = time.time()
        _download_mp3(artist, title, input_mp3)
        size_mb = os.path.getsize(input_mp3) / 1_048_576
        log.info("[1/3] DOWNLOAD  ✅ Done in %.1fs — %.2f MB", time.time() - t1, size_mb)

        # ── STEP 2: Vocal removal via audio-separator (ONNX) ──────────
        log.info("[2/3] SEPARATE  Removing vocals (%s, ONNX/CPU)...", MODEL_NAME)
        sep_dir = os.path.join(tmpdir, "separated")
        os.makedirs(sep_dir, exist_ok=True)
        t2 = time.time()

        separator = Separator(output_dir=sep_dir, output_format="mp3")
        separator.load_model(model_filename=MODEL_NAME)
        output_files = separator.separate(input_mp3)
        log.info("[2/3] SEPARATE  Raw output: %s", output_files)

        instrumental = _find_instrumental(output_files, sep_dir)
        if not os.path.exists(instrumental):
            raise ValueError(f"Instrumental file not found on disk: {instrumental}")

        size_mb2 = os.path.getsize(instrumental) / 1_048_576
        log.info("[2/3] SEPARATE  ✅ Done in %.1fs — %.2f MB → %s",
                 time.time() - t2, size_mb2, os.path.basename(instrumental))

        # ── STEP 3: Return to backend for GCS upload ──────────────────
        log.info("[3/3] RESPONSE  Sending instrumental to backend...")
        log.info("=" * 60)
        log.info("✅ PIPELINE COMPLETE: %s  (total %.1fs)", song_label, time.time() - t_start)
        log.info("=" * 60)

        background_tasks.add_task(shutil.rmtree, tmpdir, True)
        return FileResponse(instrumental, media_type="audio/mpeg", filename="instrumental.mp3")

    except Exception as e:
        shutil.rmtree(tmpdir, True)
        log.error("=" * 60)
        log.error("❌ PIPELINE FAILED: %s", song_label)
        log.error("   Reason: %s", e)
        log.error("=" * 60)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/stream")
def stream_audio(artist: str = Query(...), title: str = Query(...)):
    """Proxy-stream the original song audio from YouTube."""
    log.info("Stream request: artist=%s title=%s", artist, title)
    try:
        audio_url = search_audio_url(artist, title)
    except Exception as e:
        log.error("Failed to find audio: %s", e)
        raise HTTPException(status_code=404, detail=f"Audio not found: {e}")

    yt_headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Referer": "https://www.youtube.com/",
        "Origin": "https://www.youtube.com",
        "Accept-Language": "en-US,en;q=0.9",
    }
    upstream = requests.get(audio_url, headers=yt_headers, stream=True, timeout=10)

    def generate():
        for chunk in upstream.iter_content(chunk_size=8192):
            if chunk:
                yield chunk

    content_type = upstream.headers.get("Content-Type", "audio/mpeg")
    return StreamingResponse(generate(), media_type=content_type)


@app.get("/download")
def download_audio(
    artist: str = Query(...),
    title: str = Query(...),
    background_tasks: BackgroundTasks = BackgroundTasks(),
):
    """Download the original song as MP3 (used by the backend for GCS caching)."""
    log.info("Download request: artist=%s title=%s", artist, title)
    tmpdir = tempfile.mkdtemp()
    try:
        out = os.path.join(tmpdir, "audio.mp3")
        _download_mp3(artist, title, out)
        log.info("Downloaded %s bytes for %s - %s", os.path.getsize(out), artist, title)
        background_tasks.add_task(shutil.rmtree, tmpdir, True)
        return FileResponse(out, media_type="audio/mpeg", filename="audio.mp3")
    except Exception as e:
        shutil.rmtree(tmpdir, True)
        log.error("Download failed for %s - %s: %s", artist, title, e)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/url")
def get_audio_url(artist: str = Query(...), title: str = Query(...)):
    """Return the raw YouTube audio stream URL."""
    log.info("URL request: artist=%s title=%s", artist, title)
    try:
        audio_url = search_audio_url(artist, title)
        return {"url": audio_url}
    except Exception as e:
        log.error("Failed to find audio: %s", e)
        raise HTTPException(status_code=404, detail=f"Audio not found: {e}")
