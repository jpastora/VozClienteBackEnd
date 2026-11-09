-- =====================================================================
-- V5__restricciones_y_comentarios_modelo.sql
-- T-11 · Ajusta el esquema de V1 al modelo relacional del Informe de
-- diseño (Tabla 7 "Restricciones e índices" y Diccionario de datos).
--
--   1. Corrige los datos de prueba de V2 que no cumplen el modelo.
--   2. Valores predeterminados del diccionario de datos.
--   3. Restricciones CHECK de reglas de negocio y enumeraciones.
--   4. Índice en cada llave foránea (RNF-07).
--   5. COMMENT ON en todas las tablas y columnas (estándar del README).
--
-- Las restricciones CHECK se crean con NOT VALID: se aplican a todo
-- registro nuevo o modificado, pero no revisan las filas que cada
-- integrante ya tenga en su base local, para que la migración no falle
-- en ninguna máquina. V1 nunca se modifica (regla del README).
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Datos de prueba de V2
-- ---------------------------------------------------------------------
-- El pago simulado solo admite APROBADO o RECHAZADO (diccionario COM-07 y
-- enum EstadoPago). V2 insertó COMPLETADO y PENDIENTE, que JPA no puede leer.

UPDATE compras SET estado_pago = 'APROBADO'  WHERE estado_pago = 'COMPLETADO';
UPDATE compras SET estado_pago = 'RECHAZADO' WHERE estado_pago = 'PENDIENTE';


-- ---------------------------------------------------------------------
-- 2. Valores predeterminados del diccionario de datos
-- ---------------------------------------------------------------------

ALTER TABLE cuentas               ALTER COLUMN rol        SET DEFAULT 'CLIENTE_ADMINISTRADOR';
ALTER TABLE salas                 ALTER COLUMN estado     SET DEFAULT 'PENDIENTE';
ALTER TABLE entrevistas           ALTER COLUMN estado     SET DEFAULT 'EN_CURSO';
ALTER TABLE grabaciones           ALTER COLUMN formato    SET DEFAULT 'WEBM_OPUS';
ALTER TABLE grabaciones           ALTER COLUMN estado     SET DEFAULT 'PENDIENTE';
ALTER TABLE transcripciones       ALTER COLUMN estado     SET DEFAULT 'PENDIENTE';
ALTER TABLE transcripciones       ALTER COLUMN intentos   SET DEFAULT 0;
ALTER TABLE menciones_competencia ALTER COLUMN frecuencia SET DEFAULT 1;


-- ---------------------------------------------------------------------
-- 3. Restricciones CHECK
-- ---------------------------------------------------------------------

-- Reglas de negocio (Informe de diseño, Tabla 7)

-- Solo el administrador de la plataforma no tiene empresa.
ALTER TABLE cuentas ADD CONSTRAINT ck_cuentas_empresa
    CHECK (rol = 'ADMINISTRADOR' OR empresa_id IS NOT NULL) NOT VALID;

-- Vencimiento del enlace a los 7 días (RNF-04).
ALTER TABLE salas ADD CONSTRAINT ck_salas_vencimiento
    CHECK (vence_en = emitida_en + INTERVAL '7 days') NOT VALID;

-- Frecuencia entera mayor o igual que 1 (RF-14).
ALTER TABLE menciones_competencia ADD CONSTRAINT ck_menciones_frecuencia
    CHECK (frecuencia >= 1) NOT VALID;

-- Enumeraciones: mismos valores que los enum de Java
-- (las enumeraciones del modelo se implementan como VARCHAR con CHECK).

ALTER TABLE cuentas ADD CONSTRAINT ck_cuentas_rol
    CHECK (rol IN ('ADMINISTRADOR', 'CLIENTE_ADMINISTRADOR', 'REPRESENTANTE')) NOT VALID;

ALTER TABLE cuentas ADD CONSTRAINT ck_cuentas_estado
    CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'INACTIVA')) NOT VALID;

ALTER TABLE cambios_rol ADD CONSTRAINT ck_cambios_rol_roles
    CHECK (rol_anterior IN ('ADMINISTRADOR', 'CLIENTE_ADMINISTRADOR', 'REPRESENTANTE')
       AND rol_nuevo    IN ('ADMINISTRADOR', 'CLIENTE_ADMINISTRADOR', 'REPRESENTANTE')) NOT VALID;

ALTER TABLE compras ADD CONSTRAINT ck_compras_estado_pago
    CHECK (estado_pago IN ('APROBADO', 'RECHAZADO')) NOT VALID;

ALTER TABLE salas ADD CONSTRAINT ck_salas_estado
    CHECK (estado IN ('PENDIENTE', 'RESPONDIDO', 'EXPIRADO')) NOT VALID;

ALTER TABLE entrevistas ADD CONSTRAINT ck_entrevistas_estado
    CHECK (estado IN ('EN_CURSO', 'FINALIZADA', 'FALLIDA')) NOT VALID;

ALTER TABLE grabaciones ADD CONSTRAINT ck_grabaciones_estado
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADA', 'CON_ERROR', 'SIN_CONTENIDO')) NOT VALID;

ALTER TABLE transcripciones ADD CONSTRAINT ck_transcripciones_estado
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADA', 'CON_ERROR', 'SIN_CONTENIDO')) NOT VALID;

ALTER TABLE analisis ADD CONSTRAINT ck_analisis_estado
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADA', 'CON_ERROR')) NOT VALID;

ALTER TABLE analisis ADD CONSTRAINT ck_analisis_intencion
    CHECK (intencion IS NULL
        OR intencion IN ('RECOMPRARIA', 'NO_RECOMPRARIA', 'RECHAZA', 'NEUTRAL')) NOT VALID;

ALTER TABLE analisis ADD CONSTRAINT ck_analisis_sentimiento
    CHECK (sentimiento IS NULL
        OR sentimiento IN ('POSITIVO', 'NEGATIVO', 'NEUTRO')) NOT VALID;


-- ---------------------------------------------------------------------
-- 4. Índice en cada llave foránea (RNF-07)
-- ---------------------------------------------------------------------
-- Ya cubiertas por otros índices: cuentas.empresa_id (V3),
-- cambios_rol.cuenta_id (V3), productos.empresa_id (uq_productos_empresa_nombre)
-- y las columnas con UNIQUE de grabaciones, transcripciones y analisis.

CREATE INDEX IF NOT EXISTS idx_compras_empresa_id                  ON compras (empresa_id);
CREATE INDEX IF NOT EXISTS idx_compras_paquete_id                  ON compras (paquete_id);
CREATE INDEX IF NOT EXISTS idx_compras_cuenta_id                   ON compras (cuenta_id);
CREATE INDEX IF NOT EXISTS idx_cambios_rol_autor_id                ON cambios_rol (autor_id);
CREATE INDEX IF NOT EXISTS idx_salas_producto_id                   ON salas (producto_id);
CREATE INDEX IF NOT EXISTS idx_salas_creado_por_id                 ON salas (creado_por_id);
CREATE INDEX IF NOT EXISTS idx_entrevistas_sala_id                 ON entrevistas (sala_id);
CREATE INDEX IF NOT EXISTS idx_entrevistas_representante_id        ON entrevistas (representante_id);
CREATE INDEX IF NOT EXISTS idx_fragmentos_transcripcion_id         ON fragmentos_transcripcion (transcripcion_id);
CREATE INDEX IF NOT EXISTS idx_menciones_competencia_analisis_id   ON menciones_competencia (analisis_id);


-- ---------------------------------------------------------------------
-- 5. Documentación: COMMENT ON (texto del Diccionario de datos)
-- ---------------------------------------------------------------------

-- empresas
COMMENT ON TABLE empresas IS 'Organizaciones cliente que contratan la plataforma.';
COMMENT ON COLUMN empresas.id IS 'Identificador único de la empresa.';
COMMENT ON COLUMN empresas.nombre IS 'Nombre comercial de la empresa.';
COMMENT ON COLUMN empresas.creado_en IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN empresas.actualizado_en IS 'Fecha y hora de la última modificación.';

-- cuentas
COMMENT ON TABLE cuentas IS 'Personas que ingresan con Google: administradores, clientes-administradores y representantes.';
COMMENT ON COLUMN cuentas.id IS 'Identificador único de la cuenta.';
COMMENT ON COLUMN cuentas.empresa_id IS 'Empresa a la que pertenece la cuenta; nulo para el administrador de la plataforma.';
COMMENT ON COLUMN cuentas.nombre_completo IS 'Nombre completo de la persona (2 a 100 caracteres).';
COMMENT ON COLUMN cuentas.correo IS 'Correo de Google verificado; único en la plataforma.';
COMMENT ON COLUMN cuentas.google_sub IS 'Identificador del sujeto devuelto por Google OAuth2; se llena en el primer ingreso.';
COMMENT ON COLUMN cuentas.rol IS 'Rol de la cuenta: ADMINISTRADOR, CLIENTE_ADMINISTRADOR o REPRESENTANTE.';
COMMENT ON COLUMN cuentas.estado IS 'Estado de la cuenta: PENDIENTE, ACTIVA o INACTIVA.';
COMMENT ON COLUMN cuentas.ultimo_ingreso_en IS 'Fecha y hora del último inicio de sesión.';
COMMENT ON COLUMN cuentas.version IS 'Versión del registro para el control de concurrencia optimista.';
COMMENT ON COLUMN cuentas.creado_en IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN cuentas.actualizado_en IS 'Fecha y hora de la última modificación.';

-- cambios_rol
COMMENT ON TABLE cambios_rol IS 'Bitácora de cambios de rol realizados por el administrador de la plataforma.';
COMMENT ON COLUMN cambios_rol.id IS 'Identificador único del cambio de rol.';
COMMENT ON COLUMN cambios_rol.cuenta_id IS 'Cuenta que recibe el cambio.';
COMMENT ON COLUMN cambios_rol.autor_id IS 'Cuenta del administrador que realiza el cambio.';
COMMENT ON COLUMN cambios_rol.rol_anterior IS 'Rol previo de la cuenta.';
COMMENT ON COLUMN cambios_rol.rol_nuevo IS 'Rol asignado.';
COMMENT ON COLUMN cambios_rol.creado_en IS 'Fecha y hora del cambio.';

-- productos
COMMENT ON TABLE productos IS 'Productos de cada empresa sobre los que se entrevista a los consumidores.';
COMMENT ON COLUMN productos.id IS 'Identificador único del producto.';
COMMENT ON COLUMN productos.empresa_id IS 'Empresa propietaria del producto.';
COMMENT ON COLUMN productos.nombre IS 'Nombre del producto (3 a 100 caracteres), único dentro de la empresa.';
COMMENT ON COLUMN productos.descripcion IS 'Descripción opcional (hasta 500 caracteres).';
COMMENT ON COLUMN productos.categoria IS 'Categoría opcional del producto.';
COMMENT ON COLUMN productos.es_activo IS 'Indica si el producto admite enlaces e ingresos.';
COMMENT ON COLUMN productos.version IS 'Versión del registro para el control de concurrencia optimista.';
COMMENT ON COLUMN productos.creado_en IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN productos.actualizado_en IS 'Fecha y hora de la última modificación.';

-- salas
COMMENT ON TABLE salas IS 'Enlaces únicos de videollamada generados por producto.';
COMMENT ON COLUMN salas.id IS 'Identificador único de la sala.';
COMMENT ON COLUMN salas.producto_id IS 'Producto al que pertenece la sala.';
COMMENT ON COLUMN salas.creado_por_id IS 'Cuenta del cliente-administrador que emitió el enlace.';
COMMENT ON COLUMN salas.token IS 'Código aleatorio no adivinable (256 bits, Base64URL) que forma el enlace.';
COMMENT ON COLUMN salas.estado IS 'Estado del enlace: PENDIENTE, RESPONDIDO o EXPIRADO.';
COMMENT ON COLUMN salas.emitida_en IS 'Fecha y hora de emisión del enlace.';
COMMENT ON COLUMN salas.vence_en IS 'Fecha y hora de vencimiento (emisión más 7 días).';
COMMENT ON COLUMN salas.nombre_consumidor IS 'Nombre indicado por el consumidor (2 a 60 caracteres).';
COMMENT ON COLUMN salas.acepto_grabacion_en IS 'Fecha y hora en que el consumidor aceptó la grabación.';

-- entrevistas
COMMENT ON TABLE entrevistas IS 'Videollamadas realizadas en una sala entre un representante y el consumidor.';
COMMENT ON COLUMN entrevistas.id IS 'Identificador único de la entrevista.';
COMMENT ON COLUMN entrevistas.sala_id IS 'Sala en la que se realizó la entrevista.';
COMMENT ON COLUMN entrevistas.representante_id IS 'Cuenta del representante que condujo la entrevista.';
COMMENT ON COLUMN entrevistas.estado IS 'Estado de la llamada: EN_CURSO, FINALIZADA o FALLIDA.';
COMMENT ON COLUMN entrevistas.inicio_en IS 'Fecha y hora de inicio de la llamada.';
COMMENT ON COLUMN entrevistas.fin_en IS 'Fecha y hora de fin de la llamada.';
COMMENT ON COLUMN entrevistas.duracion_segundos IS 'Duración total de la llamada en segundos.';

-- grabaciones
COMMENT ON TABLE grabaciones IS 'Audio de cada entrevista con el canal del consumidor aislado.';
COMMENT ON COLUMN grabaciones.id IS 'Identificador único de la grabación.';
COMMENT ON COLUMN grabaciones.entrevista_id IS 'Entrevista que produjo la grabación (relación uno a uno).';
COMMENT ON COLUMN grabaciones.ruta_canal_cliente IS 'Ruta relativa del archivo con el audio del consumidor.';
COMMENT ON COLUMN grabaciones.ruta_canal_representante IS 'Ruta relativa del archivo con el audio del representante.';
COMMENT ON COLUMN grabaciones.formato IS 'Formato del audio: WEBM_OPUS o WAV.';
COMMENT ON COLUMN grabaciones.tamano_bytes IS 'Tamaño del archivo del consumidor (mayor que 0 y hasta 200 MB).';
COMMENT ON COLUMN grabaciones.duracion_segundos IS 'Duración del audio del consumidor en segundos.';
COMMENT ON COLUMN grabaciones.hash_sha256 IS 'Huella SHA-256 para verificar la integridad del archivo.';
COMMENT ON COLUMN grabaciones.tiene_audio_cliente IS 'Indica si se detectó voz en el canal del consumidor.';
COMMENT ON COLUMN grabaciones.es_parcial IS 'Indica si la grabación se cerró por un corte de conexión.';
COMMENT ON COLUMN grabaciones.estado IS 'Estado de procesamiento: PENDIENTE, EN_PROCESO, COMPLETADA, CON_ERROR o SIN_CONTENIDO.';
COMMENT ON COLUMN grabaciones.creado_en IS 'Fecha y hora de almacenamiento.';

-- transcripciones
COMMENT ON TABLE transcripciones IS 'Texto de la voz del consumidor obtenido de forma diferida con Whisper.';
COMMENT ON COLUMN transcripciones.id IS 'Identificador único de la transcripción.';
COMMENT ON COLUMN transcripciones.grabacion_id IS 'Grabación transcrita (relación uno a uno).';
COMMENT ON COLUMN transcripciones.texto IS 'Texto completo de la voz del consumidor.';
COMMENT ON COLUMN transcripciones.idioma_detectado IS 'Código del idioma detectado por Whisper (se espera es).';
COMMENT ON COLUMN transcripciones.probabilidad_idioma IS 'Probabilidad del idioma detectado, de 0 a 1.';
COMMENT ON COLUMN transcripciones.estado IS 'Estado de procesamiento: PENDIENTE, EN_PROCESO, COMPLETADA, CON_ERROR o SIN_CONTENIDO.';
COMMENT ON COLUMN transcripciones.intentos IS 'Cantidad de intentos realizados (máximo 3 automáticos).';
COMMENT ON COLUMN transcripciones.mensaje_error IS 'Detalle interno del último error; no se muestra al usuario.';
COMMENT ON COLUMN transcripciones.creado_en IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN transcripciones.actualizado_en IS 'Fecha y hora de la última modificación.';

-- fragmentos_transcripcion
COMMENT ON TABLE fragmentos_transcripcion IS 'Segmentos con marca de tiempo que devuelve el microservicio de transcripción.';
COMMENT ON COLUMN fragmentos_transcripcion.id IS 'Identificador único del fragmento.';
COMMENT ON COLUMN fragmentos_transcripcion.transcripcion_id IS 'Transcripción a la que pertenece el fragmento.';
COMMENT ON COLUMN fragmentos_transcripcion.segundo_inicio IS 'Segundo de inicio del fragmento.';
COMMENT ON COLUMN fragmentos_transcripcion.segundo_fin IS 'Segundo de fin del fragmento.';
COMMENT ON COLUMN fragmentos_transcripcion.texto IS 'Texto del fragmento.';

-- analisis
COMMENT ON TABLE analisis IS 'Resultado del análisis con IA de cada transcripción.';
COMMENT ON COLUMN analisis.id IS 'Identificador único del análisis.';
COMMENT ON COLUMN analisis.transcripcion_id IS 'Transcripción analizada (relación uno a uno).';
COMMENT ON COLUMN analisis.estado IS 'Estado de procesamiento: PENDIENTE, EN_PROCESO, COMPLETADA o CON_ERROR.';
COMMENT ON COLUMN analisis.intencion IS 'Categoría: RECOMPRARIA, NO_RECOMPRARIA, RECHAZA o NEUTRAL.';
COMMENT ON COLUMN analisis.sentimiento IS 'Sentimiento general: POSITIVO, NEGATIVO o NEUTRO.';
COMMENT ON COLUMN analisis.fragmento_apoyo IS 'Extracto breve de la transcripción que respalda la clasificación.';
COMMENT ON COLUMN analisis.tiene_menciones IS 'Indica si se detectaron menciones de la competencia.';
COMMENT ON COLUMN analisis.proveedor_ia IS 'Proveedor y modelo de IA que generó el resultado.';
COMMENT ON COLUMN analisis.intentos IS 'Cantidad de intentos realizados.';
COMMENT ON COLUMN analisis.mensaje_error IS 'Detalle interno del último error; no se muestra al usuario.';
COMMENT ON COLUMN analisis.creado_en IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN analisis.actualizado_en IS 'Fecha y hora de la última modificación.';

-- menciones_competencia
COMMENT ON TABLE menciones_competencia IS 'Marcas o soluciones competidoras detectadas en cada análisis.';
COMMENT ON COLUMN menciones_competencia.id IS 'Identificador único de la mención.';
COMMENT ON COLUMN menciones_competencia.analisis_id IS 'Análisis que detectó la mención.';
COMMENT ON COLUMN menciones_competencia.competidor IS 'Nombre normalizado de la marca (mayúsculas y espacios simples).';
COMMENT ON COLUMN menciones_competencia.frecuencia IS 'Cantidad de veces que se mencionó en la entrevista (mayor o igual que 1).';
COMMENT ON COLUMN menciones_competencia.contexto IS 'Fragmento de la transcripción donde aparece la mención.';

-- paquetes
COMMENT ON TABLE paquetes IS 'Planes de salas que ofrece la plataforma (Básico, Intermedio y Premium).';
COMMENT ON COLUMN paquetes.id IS 'Identificador único del paquete.';
COMMENT ON COLUMN paquetes.nombre IS 'Nombre único del paquete.';
COMMENT ON COLUMN paquetes.cantidad_salas IS 'Cantidad de salas que acredita (mayor o igual que 1).';
COMMENT ON COLUMN paquetes.precio IS 'Precio del paquete en colones, con dos decimales (mayor o igual que 0).';
COMMENT ON COLUMN paquetes.es_activo IS 'Indica si el paquete se ofrece a nuevas compras.';
COMMENT ON COLUMN paquetes.creado_en IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN paquetes.actualizado_en IS 'Fecha y hora de la última modificación.';

-- compras
COMMENT ON TABLE compras IS 'Adquisiciones de paquetes por parte de las empresas, con pago simulado.';
COMMENT ON COLUMN compras.id IS 'Identificador único de la compra.';
COMMENT ON COLUMN compras.empresa_id IS 'Empresa que realiza la compra.';
COMMENT ON COLUMN compras.paquete_id IS 'Paquete adquirido.';
COMMENT ON COLUMN compras.cuenta_id IS 'Cuenta del cliente-administrador que registró el pago.';
COMMENT ON COLUMN compras.monto IS 'Monto pagado al momento de la compra.';
COMMENT ON COLUMN compras.salas_acreditadas IS 'Salas acreditadas al momento de la compra.';
COMMENT ON COLUMN compras.estado_pago IS 'Resultado del pago simulado: APROBADO o RECHAZADO.';
COMMENT ON COLUMN compras.referencia_pago IS 'Referencia devuelta por la pasarela de prueba.';
COMMENT ON COLUMN compras.clave_idempotencia IS 'Clave única del formulario que evita compras duplicadas.';
COMMENT ON COLUMN compras.creado_en IS 'Fecha y hora de la compra.';
