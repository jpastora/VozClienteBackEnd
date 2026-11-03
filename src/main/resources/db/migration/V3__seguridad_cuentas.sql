-- =====================================================
-- V3__seguridad_cuentas.sql
-- =====================================================

-- Evitar correos duplicados sin distinguir mayúsculas.
-- Ejemplo: usuario@gmail.com y USUARIO@gmail.com

CREATE UNIQUE INDEX IF NOT EXISTS uq_cuentas_correo_lower
    ON cuentas (LOWER(correo));


-- Mejorar las consultas de representantes por empresa.

CREATE INDEX IF NOT EXISTS idx_cuentas_empresa_rol
    ON cuentas (empresa_id, rol);


-- Mejorar las consultas de auditoría por cuenta.

CREATE INDEX IF NOT EXISTS idx_cambios_rol_cuenta_fecha
    ON cambios_rol (cuenta_id, creado_en DESC);


-- Documentación de las tablas.

COMMENT ON TABLE cuentas IS
'Cuentas de usuarios, administradores y representantes';

COMMENT ON TABLE cambios_rol IS
'Bitácora de auditoría de cambios de roles de las cuentas';

COMMENT ON COLUMN cuentas.version IS
'Versión utilizada para control de concurrencia optimista';

COMMENT ON COLUMN cambios_rol.cuenta_id IS
'Cuenta cuyo rol fue modificado';

COMMENT ON COLUMN cambios_rol.autor_id IS
'Cuenta del administrador que realizó el cambio';

COMMENT ON COLUMN cambios_rol.rol_anterior IS
'Rol de la cuenta antes de la modificación';

COMMENT ON COLUMN cambios_rol.rol_nuevo IS
'Rol asignado después de la modificación';

COMMENT ON COLUMN cambios_rol.creado_en IS
'Fecha y hora en que se realizó el cambio';
