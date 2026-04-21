FROM python:3.11-slim

RUN apt-get update && apt-get install -y ffmpeg gcc && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .

# Install CPU-only PyTorch FIRST so audio-separator doesn't pull the 2 GB CUDA wheels
RUN pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

RUN pip install --no-cache-dir -r requirements.txt
RUN pip install --no-cache-dir --upgrade yt-dlp

# Pre-download the UVR MDX-Net vocal separation model (~65 MB)
RUN python -c "from audio_separator.separator import Separator; s = Separator(); s.load_model('UVR-MDX-NET-Inst_HQ_3.onnx'); print('Model ready.')" || echo "WARNING: model pre-download failed — will download on first use"

COPY main.py .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--timeout-keep-alive", "600"]
