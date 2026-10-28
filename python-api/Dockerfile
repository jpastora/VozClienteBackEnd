FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HOME=/models

WORKDIR /app

# Usuario no root + carpeta del caché del modelo (el volumen heredará estos permisos)
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && mkdir -p /models \
    && chown -R app:app /app /models

COPY Requirements.txt .
RUN pip install -r Requirements.txt

COPY --chown=app:app main.py .

USER app

EXPOSE 8000

# Un solo worker: cada worker cargaría su propia copia del modelo en RAM
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]