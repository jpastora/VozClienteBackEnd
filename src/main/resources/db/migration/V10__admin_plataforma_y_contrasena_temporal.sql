-- =====================================================================
-- V10__admin_plataforma_y_contrasena_temporal.sql
-- Rol de administrador de plataforma y contraseña temporal por correo.
-- =====================================================================

-- 1. Columnas para la contraseña temporal enviada por correo.
ALTER TABLE cuentas ADD COLUMN debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cuentas ADD COLUMN contrasena_temporal_vence_en TIMESTAMPTZ NULL;

COMMENT ON COLUMN cuentas.debe_cambiar_contrasena IS
'Verdadero cuando la contraseña es temporal: la persona debe cambiarla en su primer ingreso.';
COMMENT ON COLUMN cuentas.contrasena_temporal_vence_en IS
'Fecha y hora en que vence la contraseña temporal (7 días desde el envío); nulo si no hay temporal.';

-- 2. Los dos administradores existentes pasan a ser clientes-administradores
--    de su empresa (se conserva la empresa; solo cambia el rol).
UPDATE cuentas
   SET rol            = 'CLIENTE_ADMINISTRADOR',
       version        = version + 1,
       actualizado_en = CURRENT_TIMESTAMP
 WHERE LOWER(correo) IN ('cmendoza@atlas.com', 'rsolis@banco.com')
   AND rol = 'ADMINISTRADOR'
   AND empresa_id IS NOT NULL;

-- 3. Administrador de la plataforma (sin empresa). Contraseña inicial genérica
--    de desarrollo (BCrypt); debe reemplazarse antes de producción.
INSERT INTO cuentas (empresa_id, nombre_completo, correo, rol, estado, contrasena_hash)
SELECT NULL, 'Voz Cliente', 'vozclientebrightcodelab@gmail.com', 'ADMINISTRADOR', 'ACTIVA',
       '$2a$10$Lj0C6mbK/ysElje9HpSzr.QCuRK6TIIISj1ziyRq2PQUPHq1MuEB2'
 WHERE NOT EXISTS (
     SELECT 1 FROM cuentas WHERE LOWER(correo) = 'vozclientebrightcodelab@gmail.com'
 );
