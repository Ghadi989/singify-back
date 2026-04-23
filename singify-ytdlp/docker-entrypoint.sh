#!/bin/sh
set -e

# Decode YouTube cookies from Railway env vars → write to file for yt-dlp
if [ -n "$YT_COOKIES_1" ]; then
    printf '%s%s' "$YT_COOKIES_1" "${YT_COOKIES_2:-}" | base64 -d > /tmp/yt-cookies.txt
    echo "YT cookies loaded ($(wc -c < /tmp/yt-cookies.txt) bytes)"
elif [ -n "$YT_COOKIES" ]; then
    printf '%s' "$YT_COOKIES" | base64 -d > /tmp/yt-cookies.txt
    echo "YT cookies loaded ($(wc -c < /tmp/yt-cookies.txt) bytes)"
else
    echo "Warning: no YouTube cookies configured — bot detection may block requests"
fi

exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8000}" --timeout-keep-alive 600
