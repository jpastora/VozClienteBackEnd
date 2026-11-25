package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CambiarEstadoRequestDTO;
import com.eduardo.vozclientebackend.dto.CambiarRolRequestDTO;
import com.eduardo.vozclientebackend.dto.CambioRolDTO;
import com.eduardo.vozclientebackend.dto.CrearCuentaDTO;
import com.eduardo.vozclientebackend.dto.CuentaGlobalDTO;
import com.eduardo.vozclientebackend.dto.EditarCuentaDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.CambioRol;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.CambioRolRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Módulo de usuarios del administrador de la plataforma (RF-02): todas las cuentas de todas las
 * empresas.
 *
 * <ul>
 *   <li>Solo el rol ADMINISTRADOR (sin empresa) puede usarlo.</li>
 *   <li>Las cuentas nuevas nacen PENDIENTES con una contraseña temporal enviada por correo.</li>
 *   <li>Promover a ADMINISTRADOR quita la empresa; pasar un ADMINISTRADOR a otro rol exige elegir
 *       una empresa (regla {@code ck_cuentas_empresa}).</li>
 *   <li>La bitácora ({@code cambios_rol}) registra solo los cambios de rol; activar o desactivar
 *       una cuenta no se registra.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final CambioRolRepository cambioRolRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioActualService usuarioActualService;
    private final ContrasenaTemporalService contrasenaTemporalService;
    private final CorreoService correoService;
    private final Clock reloj;

    /** Crea una cuenta PENDIENTE y le envía una contraseña temporal por correo. */
    @Transactional
    public CuentaGlobalDTO crearCuenta(CrearCuentaDTO dto) {
        obtenerAdministrador();

        String correo = dto.correo().trim().toLowerCase(Locale.ROOT);
        if (cuentaRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe una cuenta registrada con este correo");
        }

        Long empresaId = dto.rol() == RolCuenta.ADMINISTRADOR ? null : exigirEmpresa(dto.empresaId());

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        Cuenta cuenta = new Cuenta();
        cuenta.setEmpresaId(empresaId);
        cuenta.setNombreCompleto(dto.nombreCompleto().trim());
        cuenta.setCorreo(correo);
        cuenta.setRol(dto.rol());
        cuenta.setEstado(EstadoCuenta.PENDIENTE);
        cuenta.setCreadoEn(ahora);
        cuenta.setActualizadoEn(ahora);
        cuenta.setVersion(null);
        boolean usaGoogle = Boolean.TRUE.equals(dto.ingresaConGoogle());
        // Con Google no hay contraseña: la persona se vincula al ingresar con ese correo.
        String contrasena = usaGoogle ? null : contrasenaTemporalService.asignar(cuenta);

        Cuenta guardada;
        try {
            guardada = cuentaRepository.saveAndFlush(cuenta);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se pudo crear la cuenta. Verifique el correo", ex);
        }

        Boolean enviado = null;
        if (!usaGoogle) {
            enviado = correoService.enviarContrasenaTemporal(
                guardada.getCorreo(), guardada.getNombreCompleto(), contrasena,
                guardada.getContrasenaTemporalVenceEn());
        }
        return aDto(guardada, nombresEmpresas(), enviado);
    }

    /** Lista todas las cuentas; si se indica {@code empresaId}, solo las de esa empresa. */
    @Transactional(readOnly = true)
    public List<CuentaGlobalDTO> listarCuentas(Long empresaId) {
        obtenerAdministrador();

        Map<Long, String> empresas = nombresEmpresas();
        return cuentaRepository.findAll(Sort.by("nombreCompleto").ascending()).stream()
            .filter(c -> empresaId == null || empresaId.equals(c.getEmpresaId()))
            .map(c -> aDto(c, empresas, null))
            .toList();
    }

    /** Cambia el rol de una cuenta y lo registra en la bitácora. */
    @Transactional
    public CuentaGlobalDTO cambiarRol(Long cuentaId, CambiarRolRequestDTO request) {
        Cuenta administrador = obtenerAdministrador();
        Cuenta cuenta = buscar(cuentaId);

        if (!Objects.equals(cuenta.getVersion(), request.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta fue modificada. Actualice la información");
        }

        RolCuenta rolAnterior = cuenta.getRol();
        RolCuenta rolNuevo = request.nuevoRol();

        if (rolAnterior == rolNuevo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya tiene el rol solicitado");
        }
        if (Objects.equals(cuenta.getId(), administrador.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes cambiar tu propio rol");
        }
        if (rolAnterior == RolCuenta.ADMINISTRADOR) {
            exigirOtroAdministradorActivo(cuenta);
        }

        if (rolNuevo == RolCuenta.ADMINISTRADOR) {
            cuenta.setEmpresaId(null);
        } else if (rolAnterior == RolCuenta.ADMINISTRADOR) {
            cuenta.setEmpresaId(exigirEmpresa(request.empresaId()));
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        cuenta.setRol(rolNuevo);
        cuenta.setActualizadoEn(ahora);
        Cuenta actualizada = cuentaRepository.saveAndFlush(cuenta);

        CambioRol cambio = new CambioRol();
        cambio.setCuentaId(actualizada.getId());
        cambio.setAutorId(administrador.getId());
        cambio.setRolAnterior(rolAnterior.name());
        cambio.setRolNuevo(rolNuevo.name());
        cambio.setCreadoEn(ahora);
        cambioRolRepository.save(cambio);

        return aDto(actualizada, nombresEmpresas(), null);
    }

    /**
     * Activa o desactiva una cuenta. Al reactivar, vuelve a PENDIENTE si la persona nunca ingresó.
     * No se registra en la bitácora (solo los cambios de rol).
     */
    @Transactional
    public CuentaGlobalDTO cambiarEstado(Long cuentaId, CambiarEstadoRequestDTO request) {
        Cuenta administrador = obtenerAdministrador();
        Cuenta cuenta = buscar(cuentaId);

        if (!Objects.equals(cuenta.getVersion(), request.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta fue modificada. Actualice la información");
        }

        if (Boolean.TRUE.equals(request.activa())) {
            if (cuenta.getEstado() != EstadoCuenta.INACTIVA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está activa");
            }
            cuenta.setEstado(cuenta.getUltimoIngresoEn() == null ? EstadoCuenta.PENDIENTE : EstadoCuenta.ACTIVA);
        } else {
            if (cuenta.getEstado() == EstadoCuenta.INACTIVA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está inactiva");
            }
            if (Objects.equals(cuenta.getId(), administrador.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta");
            }
            if (cuenta.getRol() == RolCuenta.ADMINISTRADOR) {
                exigirOtroAdministradorActivo(cuenta);
            }
            cuenta.setEstado(EstadoCuenta.INACTIVA);
        }

        cuenta.setActualizadoEn(OffsetDateTime.now(reloj));
        return aDto(cuentaRepository.saveAndFlush(cuenta), nombresEmpresas(), null);
    }

    /**
     * Edita nombre, correo y empresa de una cuenta.
     *
     * <ul>
     *   <li>El correo de una cuenta vinculada a Google no se puede cambiar: Google identifica a la persona
     *       por su identificador y el cambio dejaría la cuenta inaccesible.</li>
     *   <li>Un ADMINISTRADOR no pertenece a ninguna empresa: la empresa que llegue se ignora.</li>
     *   <li>Un representante con productos asignados no se puede mover de empresa hasta reasignarlos.</li>
     *   <li>La bitácora no registra estas ediciones (solo los cambios de rol).</li>
     * </ul>
     */
    @Transactional
    public CuentaGlobalDTO editarCuenta(Long cuentaId, EditarCuentaDTO request) {
        obtenerAdministrador();
        Cuenta cuenta = buscar(cuentaId);

        if (!Objects.equals(cuenta.getVersion(), request.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta fue modificada. Actualice la información");
        }

        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (!correo.equalsIgnoreCase(cuenta.getCorreo())) {
            if (cuenta.getGoogleSub() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta ingresa con Google: su correo no se puede cambiar");
            }
            if (cuentaRepository.existsByCorreoIgnoreCase(correo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta con este correo");
            }
            cuenta.setCorreo(correo);
        }

        if (cuenta.getRol() != RolCuenta.ADMINISTRADOR) {
            Long empresaId = exigirEmpresa(request.empresaId());
            if (!Objects.equals(empresaId, cuenta.getEmpresaId())) {
                if (cuenta.getRol() == RolCuenta.REPRESENTANTE) {
                    long productos = productoRepository.countByRepresentanteId(cuenta.getId());
                    if (productos > 0) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El representante tiene " + productos + " producto(s) asignado(s) en su empresa actual. "
                                + "Reasígnelos antes de cambiarlo de empresa");
                    }
                }
                cuenta.setEmpresaId(empresaId);
            }
        }

        cuenta.setNombreCompleto(request.nombreCompleto().trim());
        cuenta.setActualizadoEn(OffsetDateTime.now(reloj));
        try {
            return aDto(cuentaRepository.saveAndFlush(cuenta), nombresEmpresas(), null);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con este correo");
        }
    }

    /** Genera una contraseña temporal nueva y la envía por correo. */
    @Transactional
    public CuentaGlobalDTO reenviarContrasenaTemporal(Long cuentaId) {
        obtenerAdministrador();
        Cuenta cuenta = buscar(cuentaId);

        if (cuenta.getEstado() == EstadoCuenta.INACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta está inactiva. Reactívela antes de reenviar la contraseña");
        }

        String contrasena = contrasenaTemporalService.asignar(cuenta);
        cuenta.setActualizadoEn(OffsetDateTime.now(reloj));
        Cuenta guardada = cuentaRepository.saveAndFlush(cuenta);

        boolean enviado = correoService.enviarContrasenaTemporal(
            guardada.getCorreo(), guardada.getNombreCompleto(), contrasena,
            guardada.getContrasenaTemporalVenceEn());
        return aDto(guardada, nombresEmpresas(), enviado);
    }

    /** Bitácora de cambios de rol de una cuenta. */
    @Transactional(readOnly = true)
    public List<CambioRolDTO> obtenerHistorial(Long cuentaId) {
        obtenerAdministrador();
        buscar(cuentaId);

        return cambioRolRepository.findByCuentaIdOrderByCreadoEnDesc(cuentaId).stream()
            .map(c -> new CambioRolDTO(c.getId(), c.getCuentaId(), c.getAutorId(),
                c.getRolAnterior(), c.getRolNuevo(), c.getCreadoEn()))
            .toList();
    }

    // ---- Auxiliares ----

    private Cuenta obtenerAdministrador() {
        Cuenta usuario = usuarioActualService.obtenerCuentaActual();
        if (usuario.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está activa");
        }
        if (usuario.getRol() != RolCuenta.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para administrar usuarios");
        }
        return usuario;
    }

    private Cuenta buscar(Long cuentaId) {
        return cuentaRepository.findById(cuentaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    private Long exigirEmpresa(Long empresaId) {
        if (empresaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione la empresa de la cuenta");
        }
        if (!empresaRepository.existsById(empresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa seleccionada no existe");
        }
        return empresaId;
    }

    /** Impide dejar la plataforma sin administradores activos. */
    private void exigirOtroAdministradorActivo(Cuenta cuenta) {
        long activos = cuentaRepository.countByRolAndEstadoAndEmpresaIdIsNull(
            RolCuenta.ADMINISTRADOR, EstadoCuenta.ACTIVA);
        if (cuenta.getEstado() == EstadoCuenta.ACTIVA && activos <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede dejar la plataforma sin administradores activos");
        }
    }

    private Map<Long, String> nombresEmpresas() {
        Map<Long, String> nombres = new HashMap<>();
        for (Empresa e : empresaRepository.findAll()) {
            nombres.put(e.getId(), e.getNombre());
        }
        return nombres;
    }

    private static CuentaGlobalDTO aDto(Cuenta c, Map<Long, String> empresas, Boolean correoEnviado) {
        return new CuentaGlobalDTO(
            c.getId(),
            c.getEmpresaId(),
            c.getEmpresaId() == null ? null : empresas.get(c.getEmpresaId()),
            c.getNombreCompleto(),
            c.getCorreo(),
            c.getRol(),
            c.getEstado(),
            c.getUltimoIngresoEn(),
            c.getVersion(),
            c.getCreadoEn(),
            c.getActualizadoEn(),
            c.isDebeCambiarContrasena(),
            c.getGoogleSub() != null,
            correoEnviado);
    }
}
