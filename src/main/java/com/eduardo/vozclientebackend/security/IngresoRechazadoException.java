package com.eduardo.vozclientebackend.security;

/**
 * Se lanza cuando un ingreso con Google no puede completarse (HU-01).
 *
 * <p>El motivo viaja al front-end como código en la URL ({@code /auth?error=...}), sin
 * datos técnicos ni credenciales (RF-01, manejo de excepciones).</p>
 */
public class IngresoRechazadoException extends RuntimeException {

    private final Motivo motivo;

    /**
     * Crea la excepción.
     *
     * @param motivo causa del rechazo
     */
    public IngresoRechazadoException(Motivo motivo) {
        super(motivo.getCodigo());
        this.motivo = motivo;
    }

    /**
     * Causa del rechazo.
     *
     * @return el motivo
     */
    public Motivo getMotivo() {
        return motivo;
    }

    /** Causas por las que se rechaza un ingreso. */
    public enum Motivo {
        /** Google no devolvió el correo o el identificador. */
        DATOS_INCOMPLETOS("datos_incompletos"),
        /** El correo de Google no está verificado (RF-01, validaciones). */
        CORREO_NO_VERIFICADO("correo_no_verificado"),
        /** La cuenta existe pero está inactiva (RF-01, excepciones). */
        CUENTA_INACTIVA("cuenta_inactiva"),
        /** El correo ya está vinculado a otra cuenta de Google. */
        CUENTA_VINCULADA_A_OTRO("cuenta_vinculada_a_otro");

        private final String codigo;

        Motivo(String codigo) {
            this.codigo = codigo;
        }

        /**
         * Código que recibe el front-end.
         *
         * @return el código en minúsculas
         */
        public String getCodigo() {
            return codigo;
        }
    }
}
