-- =====================================================
-- V4__asignar_google_sub_cuenta_dev_test.sql
-- =====================================================

-- Identidad de pruebas locales.
-- El perfil "dev-test" autentica todas las peticiones como el usuario
-- con google_sub = 'dev-test-admin' (ver DevTestAuthenticationFilter).
-- UsuarioActualService busca la cuenta por ese google_sub, por lo que debe
-- existir una cuenta con empresa asignada para que los servicios que
-- dependen de la empresa del usuario (productos, etc.) funcionen.
--
-- Se asocia a la cuenta 1 (Carlos Mendoza, empresa 1) de V2.
-- Es idempotente: no hace nada si la cuenta no existe, si ya tiene un
-- google_sub, o si 'dev-test-admin' ya está asignado a otra cuenta.

UPDATE cuentas
SET google_sub = 'dev-test-admin'
WHERE id = 1
  AND google_sub IS NULL
  AND empresa_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM cuentas WHERE google_sub = 'dev-test-admin');


-- Un google_sub identifica a una sola cuenta.
-- CuentaRepository.findByGoogleSub devuelve un único resultado, así que un
-- valor repetido provocaría errores al iniciar sesión.
-- Índice parcial: permite varias cuentas sin google_sub (aún no han iniciado sesión).

CREATE UNIQUE INDEX IF NOT EXISTS uq_cuentas_google_sub
    ON cuentas (google_sub)
    WHERE google_sub IS NOT NULL;


COMMENT ON COLUMN cuentas.google_sub IS
'Identificador "sub" de la cuenta de Google; vincula el inicio de sesión con la cuenta';
