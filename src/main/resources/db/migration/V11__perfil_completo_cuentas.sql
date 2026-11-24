-- Perfil de la cuenta: quien ingresa por primera vez con Google debe completar su nombre y el de su
-- empresa antes de usar la aplicación. Las cuentas que ya existen quedan con el perfil completo.
ALTER TABLE cuentas
    ADD COLUMN perfil_completo BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN cuentas.perfil_completo IS
    'Falso mientras la persona no complete su nombre y el de su empresa tras el primer ingreso con Google.';
