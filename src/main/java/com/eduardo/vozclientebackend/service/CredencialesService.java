package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.IngresoDTO;
import com.eduardo.vozclientebackend.dto.RegistroDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.security.LimiteIntentosIngreso;
import com.eduardo.vozclientebackend.security.PoliticaContrasena;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Ingreso y registro con correo y contraseña propios de la aplicación, además de Google (HU-01).
 *
 * <ul>
 *   <li>La contraseña nunca se guarda en texto plano: solo su hash BCrypt.</li>
 *   <li>Un correo o una contraseña incorrectos responden el mismo mensaje, para no revelar qué
 *       correos existen.</li>
 *   <li>Después de varios intentos fallidos con el mismo correo, el ingreso se bloquea un tiempo
 *       ({@link LimiteIntentosIngreso}).</li>
 *   <li>El registro crea la empresa y la cuenta como cliente-administrador. No toma cuentas que ya
 *       existen (por ejemplo, un representante registrado por su empresa): esa persona ingresa con
 *       Google.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CredencialesService {

    /** Mensaje único para correo inexistente, cuenta sin contraseña o contraseña incorrecta. */
    static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos";

    private static final int LARGO_MAXIMO_EMPRESA = 150;

    /** Hash BCrypt válido de una contraseña aleatoria que nadie conoce; solo iguala el tiempo de respuesta. */
    private static final String HASH_FICTICIO =
        "$2a$10$PTVQy0r7QanNa0X7GmLNyOorOS3QaBQqSFIwsOTPBiWmF6kU/9g2e";

    private final CuentaRepository cuentaRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final LimiteIntentosIngreso limiteIntentos;
    private final Clock reloj;

    /**
     * Valida el correo y la contraseña y devuelve la cuenta lista para abrir la sesión.
     *
     * @param datos correo y contraseña
     * @return la cuenta activa
     * @throws ResponseStatusException 401 si el correo o la contraseña no son válidos;
     *                                 403 si la cuenta está inactiva;
     *                                 429 si hubo demasiados intentos fallidos con ese correo
     */
    @Transactional
    public Cuenta ingresar(IngresoDTO datos) {
        String correo = normalizar(datos.correo());
        // Si el correo está bloqueado no se revisa la contraseña: así no se puede seguir probando.
        limiteIntentos.verificar(correo);

        Optional<Cuenta> encontrada = cuentaRepository.buscarPorCorreo(correo);
        String hash = encontrada.map(Cuenta::getContrasenaHash).orElse(null);

        if (hash == null) {
            // Se compara igual contra un hash ficticio para que la respuesta tarde lo mismo exista o no
            // el correo, y así no se pueda averiguar qué correos están registrados.
            passwordEncoder.matches(datos.contrasena(), HASH_FICTICIO);
            limiteIntentos.registrarFallo(correo);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }
        if (!passwordEncoder.matches(datos.contrasena(), hash)) {
            limiteIntentos.registrarFallo(correo);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }
        limiteIntentos.registrarExito(correo);

        Cuenta cuenta = encontrada.get();
        if (cuenta.getEstado() == EstadoCuenta.INACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }
        if (cuenta.getEstado() == EstadoCuenta.PENDIENTE) {
            // Cuenta pendiente que ingresa por primera vez.
            cuenta.setEstado(EstadoCuenta.ACTIVA);
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        cuenta.setUltimoIngresoEn(ahora);
        cuenta.setActualizadoEn(ahora);
        return cuentaRepository.save(cuenta);
    }

    /**
     * Crea la empresa y la cuenta de cliente-administrador con correo y contraseña.
     *
     * @param datos nombre, correo y contraseña
     * @return la cuenta creada, activa
     * @throws ResponseStatusException 400 si la contraseña no cumple la política;
     *                                 409 si el correo ya está registrado
     */
    @Transactional
    public Cuenta registrar(RegistroDTO datos) {
        PoliticaContrasena.validar(datos.contrasena());

        String correo = normalizar(datos.correo());
        if (cuentaRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe una cuenta con este correo. Ingrese con su contraseña o con Google");
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        String nombre = datos.nombreCompleto().trim();

        Empresa empresa = new Empresa();
        String nombreEmpresa = "Empresa de " + nombre;
        empresa.setNombre(nombreEmpresa.length() <= LARGO_MAXIMO_EMPRESA
            ? nombreEmpresa : nombreEmpresa.substring(0, LARGO_MAXIMO_EMPRESA));
        empresa.setCreadoEn(ahora);
        empresa.setActualizadoEn(ahora);
        Empresa empresaGuardada = empresaRepository.save(empresa);

        Cuenta cuenta = new Cuenta();
        cuenta.setEmpresaId(empresaGuardada.getId());
        cuenta.setNombreCompleto(nombre);
        cuenta.setCorreo(correo);
        cuenta.setContrasenaHash(passwordEncoder.encode(datos.contrasena()));
        cuenta.setRol(RolCuenta.CLIENTE_ADMINISTRADOR);
        cuenta.setEstado(EstadoCuenta.ACTIVA);
        cuenta.setUltimoIngresoEn(ahora);
        cuenta.setCreadoEn(ahora);
        cuenta.setActualizadoEn(ahora);
        return cuentaRepository.save(cuenta);
    }

    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

}
