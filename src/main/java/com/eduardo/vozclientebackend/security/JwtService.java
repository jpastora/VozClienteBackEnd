package com.eduardo.vozclientebackend.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y valida el JWT propio de Voz Cliente (HU-01).
 *
 * <p>El token se firma con HMAC-SHA256 y solo guarda el id de la cuenta ({@code sub}), que
 * sirve igual para el ingreso con Google y con correo y contraseña. El rol y el estado se leen de la base de datos en cada
 * solicitud, de modo que un cambio de rol o una desactivación aplican de inmediato (RF-02).
 * La sesión expira por inactividad: cada solicitud renueva el token (RNF de seguridad).</p>
 */
@Service
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    /** Emisor que se escribe y se exige en cada token. */
    static final String EMISOR = "vozcliente";

    /** Longitud mínima del secreto para HS256: 256 bits. */
    private static final int BYTES_MINIMOS_SECRETO = 32;

    private final byte[] secreto;
    private final Duration duracionInactividad;
    private final Clock reloj;

    /**
     * Crea el servicio a partir de la configuración.
     *
     * @param secretoConfigurado  valor de {@code JWT_SECRET}; si está vacío se genera uno aleatorio
     * @param minutosInactividad  minutos sin actividad tras los cuales expira la sesión
     */
    @Autowired
    public JwtService(
        @Value("${app.seguridad.jwt-secreto:}") String secretoConfigurado,
        @Value("${app.seguridad.minutos-inactividad:30}") long minutosInactividad) {

        this(secretoConfigurado, Duration.ofMinutes(minutosInactividad), Clock.systemUTC());
    }

    /**
     * Constructor para pruebas, con reloj controlado.
     *
     * @param secretoConfigurado  secreto de firma
     * @param duracionInactividad tiempo de vida del token
     * @param reloj               reloj usado para las fechas del token
     */
    JwtService(String secretoConfigurado, Duration duracionInactividad, Clock reloj) {
        this.secreto = resolverSecreto(secretoConfigurado);
        this.duracionInactividad = duracionInactividad;
        this.reloj = reloj;
    }

    /**
     * Emite un token nuevo para la cuenta.
     *
     * @param cuentaId id de la cuenta
     * @return el JWT firmado, en formato compacto
     * @throws IllegalStateException si no se puede firmar el token
     */
    public String emitir(Long cuentaId) {
        return emitir(String.valueOf(cuentaId));
    }

    /**
     * Emite un token con el sujeto indicado.
     *
     * @param sujeto valor del claim {@code sub}
     * @return el JWT firmado, en formato compacto
     */
    String emitir(String sujeto) {
        Instant ahora = reloj.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(sujeto)
            .issuer(EMISOR)
            .issueTime(Date.from(ahora))
            .expirationTime(Date.from(ahora.plus(duracionInactividad)))
            .build();

        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de sesión", e);
        }
    }

    /**
     * Valida la firma, el emisor y la vigencia de un token.
     *
     * @param token JWT en formato compacto
     * @return los datos de la sesión, o vacío si el token no es válido o ya expiró
     */
    public Optional<SesionToken> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                || !jwt.verify(new MACVerifier(secreto))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date emitido = claims.getIssueTime();
            Date expira = claims.getExpirationTime();
            String sub = claims.getSubject();
            if (!EMISOR.equals(claims.getIssuer()) || sub == null || emitido == null || expira == null) {
                return Optional.empty();
            }
            if (!expira.toInstant().isAfter(reloj.instant())) {
                return Optional.empty();
            }
            return Optional.of(new SesionToken(sub, emitido.toInstant(), expira.toInstant()));
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    /**
     * Indica si conviene renovar el token: cuando ya pasó la mitad de su vida.
     * Así la sesión se extiende con la actividad sin firmar un token en cada solicitud.
     *
     * @param sesion datos del token actual
     * @return {@code true} si debe emitirse un token nuevo
     */
    public boolean debeRenovarse(SesionToken sesion) {
        Instant mitad = sesion.emitidoEn().plus(duracionInactividad.dividedBy(2));
        return !reloj.instant().isBefore(mitad);
    }

    /**
     * Tiempo de vida del token y de la cookie.
     *
     * @return duración de la sesión sin actividad
     */
    public Duration getDuracionInactividad() {
        return duracionInactividad;
    }

    private static byte[] resolverSecreto(String configurado) {
        if (configurado == null || configurado.isBlank()) {
            LOG.warn("JWT_SECRET no está configurado: se usa un secreto aleatorio y las sesiones "
                + "se cerrarán al reiniciar el back-end. Configúrelo en el archivo .env.");
            byte[] aleatorio = new byte[BYTES_MINIMOS_SECRETO];
            new SecureRandom().nextBytes(aleatorio);
            return aleatorio;
        }
        byte[] bytes = configurado.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException(
                "JWT_SECRET debe tener al menos " + BYTES_MINIMOS_SECRETO + " caracteres (256 bits)");
        }
        return bytes;
    }

    /**
     * Datos de una sesión válida.
     *
     * @param sujeto    claim {@code sub}: el id de la cuenta
     * @param emitidoEn fecha de emisión del token
     * @param expiraEn  fecha de expiración del token
     */
    public record SesionToken(String sujeto, Instant emitidoEn, Instant expiraEn) {

        /**
         * Id de la cuenta del token.
         *
         * @return el id, o vacío si el sujeto no es un id (por ejemplo, un token de una versión anterior)
         */
        public Optional<Long> cuentaId() {
            try {
                return Optional.of(Long.valueOf(sujeto));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
}
