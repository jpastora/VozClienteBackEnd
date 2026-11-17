-- =====================================================================
-- V9__grabacion_por_fragmentos.sql
-- HU-10 · La grabación se recibe por fragmentos mientras la llamada sigue
-- en curso, para no perder lo ya grabado si se corta la conexión.
--
--   1. Nuevo estado GRABANDO (la fila existe antes de que termine la llamada).
--   2. tamano_bytes puede ser 0 y hash_sha256 se calcula al finalizar.
--   3. Control de secuencia y de actividad por canal.
-- =====================================================================

-- 1. Estado GRABANDO
ALTER TABLE grabaciones DROP CONSTRAINT ck_grabaciones_estado;
ALTER TABLE grabaciones ADD CONSTRAINT ck_grabaciones_estado
    CHECK (estado IN ('PENDIENTE', 'GRABANDO', 'EN_PROCESO', 'COMPLETADA', 'CON_ERROR', 'SIN_CONTENIDO')) NOT VALID;

-- 2. Durante la grabación todavía no hay tamaño final ni huella.
--    (V1 exigía tamano_bytes > 0; ahora se permite 0 hasta que llegue el primer fragmento.)
ALTER TABLE grabaciones DROP CONSTRAINT ck_grabaciones_tamano;
ALTER TABLE grabaciones ADD CONSTRAINT ck_grabaciones_tamano
    CHECK (tamano_bytes >= 0 AND tamano_bytes <= 209715200);

ALTER TABLE grabaciones ALTER COLUMN hash_sha256       DROP NOT NULL;
ALTER TABLE grabaciones ALTER COLUMN tamano_bytes      SET DEFAULT 0;
ALTER TABLE grabaciones ALTER COLUMN duracion_segundos SET DEFAULT 0;

-- 3. Control de fragmentos
ALTER TABLE grabaciones ADD COLUMN ultima_secuencia_cliente       INTEGER     NOT NULL DEFAULT -1;
ALTER TABLE grabaciones ADD COLUMN ultima_secuencia_representante INTEGER     NOT NULL DEFAULT -1;
ALTER TABLE grabaciones ADD COLUMN bytes_canal_representante      BIGINT      NOT NULL DEFAULT 0;
ALTER TABLE grabaciones ADD COLUMN ultimo_fragmento_en            TIMESTAMPTZ NULL;

-- El planificador busca grabaciones GRABANDO sin actividad reciente.
CREATE INDEX IF NOT EXISTS idx_grabaciones_estado_ultimo_fragmento
    ON grabaciones (estado, ultimo_fragmento_en);

COMMENT ON COLUMN grabaciones.estado IS 'Estado: PENDIENTE, GRABANDO, EN_PROCESO, COMPLETADA, CON_ERROR o SIN_CONTENIDO.';
COMMENT ON COLUMN grabaciones.hash_sha256 IS 'Huella SHA-256 del canal del consumidor; se calcula al finalizar la grabación.';
COMMENT ON COLUMN grabaciones.ultima_secuencia_cliente IS 'Número del último fragmento aceptado del canal del consumidor (-1 si no ha llegado ninguno).';
COMMENT ON COLUMN grabaciones.ultima_secuencia_representante IS 'Número del último fragmento aceptado del canal del representante (-1 si no ha llegado ninguno).';
COMMENT ON COLUMN grabaciones.bytes_canal_representante IS 'Bytes confirmados del archivo del representante.';
COMMENT ON COLUMN grabaciones.ultimo_fragmento_en IS 'Fecha y hora del último fragmento recibido; permite cerrar grabaciones abandonadas.';
