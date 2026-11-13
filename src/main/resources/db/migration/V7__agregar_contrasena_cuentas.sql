-- =====================================================================
-- V7__agregar_contrasena_cuentas.sql
-- Ingreso con correo y contraseña propios de la aplicación, además de Google.
-- La contraseña nunca se guarda en texto plano: solo su hash BCrypt.
-- =====================================================================

ALTER TABLE cuentas ADD COLUMN contrasena_hash VARCHAR(100) NULL;

COMMENT ON COLUMN cuentas.contrasena_hash IS
'Hash BCrypt de la contraseña propia de la aplicación; nulo si la cuenta solo ingresa con Google';
