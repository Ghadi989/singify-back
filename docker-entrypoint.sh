#!/bin/sh
set -e

# Decode base64 GCS credentials — supports single var or split across two vars
if [ -n "$GCS_CREDS" ]; then
    printf '%s' "$GCS_CREDS" | base64 -d > /tmp/gcs-creds.json
    export GCS_CREDENTIALS_PATH=/tmp/gcs-creds.json
    echo "GCS: loaded from GCS_CREDS ($(wc -c < /tmp/gcs-creds.json) bytes)"
elif [ -n "$GCS_CREDS_1" ] && [ -n "$GCS_CREDS_2" ]; then
    printf '%s%s' "$GCS_CREDS_1" "$GCS_CREDS_2" | base64 -d > /tmp/gcs-creds.json
    export GCS_CREDENTIALS_PATH=/tmp/gcs-creds.json
    echo "GCS: loaded from GCS_CREDS_1+2 ($(wc -c < /tmp/gcs-creds.json) bytes)"
else
    echo "GCS: no credentials env var found"
fi

exec java -jar /app/app.jar
