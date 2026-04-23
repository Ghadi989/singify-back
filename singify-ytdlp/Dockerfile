FROM python:3.11-slim

RUN apt-get update && apt-get install -y ffmpeg gcc && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .

# Install CPU-only PyTorch FIRST so audio-separator doesn't pull the 2 GB CUDA wheels
RUN pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

RUN pip install --no-cache-dir -r requirements.txt

# yt-dlp must be latest — it updates daily to track YouTube API changes
ARG YTDLP_CACHE_BUST=1
RUN pip install --no-cache-dir --upgrade yt-dlp

# Pre-download the UVR MDX-Net vocal separation model (~65 MB)
RUN python -c "from audio_separator.separator import Separator; s = Separator(); s.load_model('UVR-MDX-NET-Inst_HQ_3.onnx'); print('Model ready.')" || echo "WARNING: model pre-download failed — will download on first use"

COPY main.py .
COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

EXPOSE 8000

CMD ["/docker-entrypoint.sh"]
