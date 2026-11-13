package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.security.IngresoRechazadoException;
import com.eduardo.vozclientebackend.security.IngresoRechazadoException.Motivo;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Registra el ingreso de una persona que se autenticó con Google (HU-01, RF-01).
 *
 * <ul>
 *   <li>Si la cuenta ya está vinculada a ese usuario de Google, actualiza su último ingreso.</li>
 *   <li>Si el correo ya existe sin vincular (por ejemplo, un representante creado con RF-03),
 *       vincula la cuenta y la activa.</li>
 *   <li>Si esa cuenta sin vincular tenía contraseña, la contraseña se borra: Google acaba de
 *       confirmar quién es el dueño del correo, pero no se sabe quién creó esa contraseña (el
 *       registro con contraseña todavía no verifica el correo). Así nadie que haya registrado un
 *       correo ajeno conserva el acceso. Desde ese momento la cuenta ingresa con Google.</li>
 *   <li>Si es el primer ingreso, crea la empresa y la cuenta como cliente-administrador.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RegistroGoogleService {

    private static final int LARGO_MAXIMO_NOMBRE = 100;
    private static final int LARGO_MINIMO_NOMBRE = 2;
    private static final int LARGO_MAXIMO_EMPRESA = 150;

    private final CuentaRepository cuentaRepository;
    private final EmpresaRepository empresaRepository;
    private final Clock reloj;

    /**
     * Registra el ingreso y devuelve la cuenta lista para abrir la sesión.
     *
     * @param googleSub        identificador del sujeto devuelto por Google
     * @param correo           correo de la cuenta de Google
     * @param nombre           nombre completo devuelto por Google (puede venir vacío)
     * @param correoVerificado indica si Google verificó el correo
     * @return la cuenta activa asociada al usuario de Google
     * @throws IngresoRechazadoException si faltan datos, el correo no está verificado, la cuenta
     *                                   está inactiva o el correo pertenece a otra cuenta de Google
     */
    @Transactional
    public Cuenta registrarIngreso(String googleSub, String correo, String nombre, boolean correoVerificado) {
        if (esVacio(googleSub) || esVacio(correo)) {
            throw new IngresoRechazadoException(Motivo.DATOS_INCOMPLETOS);
        }
        if (!correoVerificado) {
            throw new IngresoRechazadoException(Motivo.CORREO_NO_VERIFICADO);
        }

        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        Cuenta cuenta = buscarOVincular(googleSub, correoNormalizado)
            .orElseGet(() -> crearClienteAdministrador(googleSub, correoNormalizado, nombre, ahora));

        if (cuenta.getEstado() == EstadoCuenta.INACTIVA) {
            throw new IngresoRechazadoException(Motivo.CUENTA_INACTIVA);
        }
        if (cuenta.getEstado() == EstadoCuenta.PENDIENTE) {
            // Representante o cuenta creada por un administrador que ingresa por primera vez.
            cuenta.setEstado(EstadoCuenta.ACTIVA);
        }

        cuenta.setUltimoIngresoEn(ahora);
        cuenta.setActualizadoEn(ahora);
        return cuentaRepository.save(cuenta);
    }

    private Optional<Cuenta> buscarOVincular(String googleSub, String correo) {
        Optional<Cuenta> porSub = cuentaRepository.findByGoogleSub(googleSub);
        if (porSub.isPresent()) {
            return porSub;
        }

        Optional<Cuenta> porCorreo = cuentaRepository.buscarPorCorreo(correo);
        porCorreo.ifPresent(cuenta -> {
            if (cuenta.getGoogleSub() != null && !cuenta.getGoogleSub().equals(googleSub)) {
                throw new IngresoRechazadoException(Motivo.CUENTA_VINCULADA_A_OTRO);
            }
            if (cuenta.getGoogleSub() == null) {
                cuenta.setGoogleSub(googleSub);
                cuenta.setContrasenaHash(null);
            }
        });
        return porCorreo;
    }

    private Cuenta crearClienteAdministrador(String googleSub, String correo, String nombre,
                                             OffsetDateTime ahora) {
        String nombreCompleto = normalizarNombre(nombre, correo);

        Empresa empresa = new Empresa();
        empresa.setNombre(recortar("Empresa de " + nombreCompleto, LARGO_MAXIMO_EMPRESA));
        empresa.setCreadoEn(ahora);
        empresa.setActualizadoEn(ahora);
        Empresa empresaGuardada = empresaRepository.save(empresa);

        Cuenta cuenta = new Cuenta();
        cuenta.setEmpresaId(empresaGuardada.getId());
        cuenta.setNombreCompleto(nombreCompleto);
        cuenta.setCorreo(correo);
        cuenta.setGoogleSub(googleSub);
        cuenta.setRol(RolCuenta.CLIENTE_ADMINISTRADOR);
        cuenta.setEstado(EstadoCuenta.ACTIVA);
        cuenta.setCreadoEn(ahora);
        cuenta.setActualizadoEn(ahora);
        return cuenta;
    }

    /**
     * Usa el nombre de Google; si falta o es muy corto, usa la parte local del correo
     * (diccionario CTA-03: de 2 a 100 caracteres).
     */
    private static String normalizarNombre(String nombre, String correo) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.length() < LARGO_MINIMO_NOMBRE) {
            limpio = correo.substring(0, correo.indexOf('@') > 0 ? correo.indexOf('@') : correo.length());
        }
        if (limpio.length() < LARGO_MINIMO_NOMBRE) {
            limpio = correo;
        }
        return recortar(limpio, LARGO_MAXIMO_NOMBRE);
    }

    private static String recortar(String texto, int largoMaximo) {
        return texto.length() <= largoMaximo ? texto : texto.substring(0, largoMaximo);
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
