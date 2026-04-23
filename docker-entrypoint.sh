#!/bin/sh
set -e

# Decode base64 GCS credentials from Railway env var → write to temp file
if [ -n "$GCS_CREDS" ]; then
    echo "$GCS_CREDS" | base64 -d > /tmp/gcs-creds.json
    export GCS_CREDENTIALS_PATH=/tmp/gcs-creds.json
fi

exec java -jar /app/app.jar
