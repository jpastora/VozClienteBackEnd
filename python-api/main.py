import logging
import os
import shutil
import tempfile
import threading

from av.error import FFmpegError
from fastapi import FastAPI, File, HTTPException, UploadFile
from faster_whisper import WhisperModel

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger("whisper-api")

# Configuración por variables de entorno (se pueden cambiar desde docker-compose)
MODEL_SIZE = os.getenv("WHISPER_MODEL", "small")          # tiny, base, small, medium, large-v3
DEVICE = os.getenv("WHISPER_DEVICE", "cpu")               # cpu o cuda
COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "int8")  # int8 va bien en CPU
LANGUAGE = os.getenv("WHISPER_LANGUAGE", "es") or None    # None = detección automática
CPU_THREADS = int(os.getenv("WHISPER_CPU_THREADS", "0"))  # 0 = valor por defecto de la librería
MAX_CONCURRENT = int(os.getenv("WHISPER_MAX_CONCURRENT", "1"))  # transcripciones simultáneas

app = FastAPI(title="Whisper API", version="1.1.0")

# Limita las transcripciones simultáneas para que no compitan por los mismos núcleos.
# Las demás peticiones esperan su turno.
semaforo = threading.Semaphore(MAX_CONCURRENT)

# El modelo se carga una sola vez al iniciar (la primera vez lo descarga)
logger.info(
    "Cargando modelo Whisper '%s' en %s (%s), cpu_threads=%s, concurrencia=%s...",
    MODEL_SIZE, DEVICE, COMPUTE_TYPE, CPU_THREADS, MAX_CONCURRENT,
)
model = WhisperModel(
    MODEL_SIZE,
    device=DEVICE,
    compute_type=COMPUTE_TYPE,
    cpu_threads=CPU_THREADS,
)
logger.info("Modelo listo.")


@app.get("/health")
def health():
    return {"status": "ok", "modelo": MODEL_SIZE}


@app.post("/transcribir")
def transcribir(file: UploadFile = File(...)):
    """
    Recibe un archivo de audio o video (mp3, wav, mp4, mkv, webm, etc.)
    y devuelve la transcripción completa y por segmentos.
    """
    suffix = os.path.splitext(file.filename or "")[1] or ".tmp"

    # Guardamos el archivo en disco temporal para que lo lea Whisper
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        shutil.copyfileobj(file.file, tmp)
        ruta_temporal = tmp.name

    try:
        # Solo una (o MAX_CONCURRENT) transcripción a la vez
        with semaforo:
            segmentos, info = model.transcribe(
                ruta_temporal,
                language=LANGUAGE,
                beam_size=5,
                vad_filter=True,  # ignora silencios largos
            )

            lista_segmentos = []
            textos = []
            # 'segmentos' es un generador: aquí ocurre la transcripción real
            for s in segmentos:
                texto = s.text.strip()
                if texto:
                    textos.append(texto)
                    lista_segmentos.append(
                        {"inicio": round(s.start, 2), "fin": round(s.end, 2), "texto": texto}
                    )

        idioma = getattr(info, "language", "es") or "es"
        probabilidad = round(getattr(info, "language_probability", 1.0) or 1.0, 3)
        duracion = round(getattr(info, "duration", 0.0) or 0.0, 2)

        return {
            "texto": " ".join(textos),
            "idioma": idioma,
            "probabilidadIdioma": probabilidad,
            "duracionSegundos": duracion,
            "segmentos": lista_segmentos,
        }
    except (FFmpegError, ValueError) as e:
        # Archivo corrupto, formato no soportado o sin pista de audio: error del cliente
        logger.warning("Archivo inválido '%s': %s", file.filename, e)
        raise HTTPException(
            status_code=400,
            detail=f"No se pudo leer el archivo: {e}. Verifique que contenga una pista de audio válida con voz.",
        )
    except Exception:
        # Cualquier otro fallo (memoria, bug, etc.): error del servidor, con traceback en logs
        logger.exception("Error interno al transcribir '%s'", file.filename)
        raise HTTPException(
            status_code=500,
            detail="Error interno durante la transcripción.",
        )
    finally:
        if os.path.exists(ruta_temporal):
            os.remove(ruta_temporal)