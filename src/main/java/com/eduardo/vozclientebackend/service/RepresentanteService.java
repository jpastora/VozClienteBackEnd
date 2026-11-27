package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ActualizarRepresentanteDTO;
import com.eduardo.vozclientebackend.dto.CrearRepresentanteDTO;
import com.eduardo.vozclientebackend.dto.CuentaDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.mappers.CuentaMapper;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RepresentanteService {

    private final CuentaRepository cuentaRepository;
    private final CuentaMapper cuentaMapper;
    private final UsuarioActualService usuarioActualService;
    private final ContrasenaTemporalService contrasenaTemporalService;
    private final CorreoService correoService;

    @Transactional
    public CuentaDTO crearRepresentante(CrearRepresentanteDTO dto) {

        Long empresaId = obtenerEmpresaAutorizada();

        String correo = dto.correo()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (cuentaRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una cuenta registrada con este correo"
            );
        }

        Cuenta representante = new Cuenta();

        representante.setEmpresaId(empresaId);
        representante.setNombreCompleto(dto.nombreCompleto().trim());
        representante.setCorreo(correo);
        representante.setRol(RolCuenta.REPRESENTANTE);
        // Pendiente hasta su primer ingreso (con Google o con la contraseña temporal que recibe por correo).
        representante.setEstado(EstadoCuenta.PENDIENTE);
        boolean usaGoogle = Boolean.TRUE.equals(dto.ingresaConGoogle());
        String contrasenaTemporal = usaGoogle ? null : contrasenaTemporalService.asignar(representante);

        // Si usa Google, vinculará su cuenta al iniciar sesión.
        representante.setGoogleSub(null);
        representante.setUltimoIngresoEn(null);

        OffsetDateTime ahora = OffsetDateTime.now();

        representante.setCreadoEn(ahora);
        representante.setActualizadoEn(ahora);

        // La versión inicial se administra con JPA @Version.
        representante.setVersion(null);

        try {
            Cuenta guardada = cuentaRepository.saveAndFlush(representante);
            Boolean enviado = null;
            if (!usaGoogle) {
                enviado = correoService.enviarContrasenaTemporal(
                    guardada.getCorreo(), guardada.getNombreCompleto(), contrasenaTemporal,
                    guardada.getContrasenaTemporalVenceEn());
            }
            return conEnvio(cuentaMapper.toDto(guardada), enviado);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo crear el representante. Verifique el correo",
                ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<CuentaDTO> listarRepresentantes() {

        Long empresaId = obtenerEmpresaAutorizada();

        List<Cuenta> representantes =
            cuentaRepository.findByEmpresaIdAndRolOrderByNombreCompletoAsc(
                empresaId,
                RolCuenta.REPRESENTANTE
            );

        return cuentaMapper.toDtoList(representantes);
    }

    @Transactional(readOnly = true)
    public CuentaDTO obtenerRepresentante(Long id) {

        Long empresaId = obtenerEmpresaAutorizada();

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(
                id,
                empresaId,
                RolCuenta.REPRESENTANTE
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        return cuentaMapper.toDto(representante);
    }

    /** Genera una contraseña temporal nueva para un representante de la empresa y la envía por correo. */
    @Transactional
    public CuentaDTO reenviarContrasenaTemporal(Long id) {

        Long empresaId = obtenerEmpresaAutorizada();

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(id, empresaId, RolCuenta.REPRESENTANTE)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        if (representante.getEstado() == EstadoCuenta.INACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El representante está desactivado"
            );
        }

        String contrasena = contrasenaTemporalService.asignar(representante);
        representante.setActualizadoEn(OffsetDateTime.now());
        Cuenta guardada = cuentaRepository.saveAndFlush(representante);

        boolean enviado = correoService.enviarContrasenaTemporal(
            guardada.getCorreo(), guardada.getNombreCompleto(), contrasena,
            guardada.getContrasenaTemporalVenceEn());
        return conEnvio(cuentaMapper.toDto(guardada), enviado);
    }

    private static CuentaDTO conEnvio(CuentaDTO d, Boolean enviado) {
        return new CuentaDTO(d.id(), d.empresaId(), d.nombreCompleto(), d.correo(), d.googleSub(),
            d.rol(), d.estado(), d.ultimoIngresoEn(), d.version(), d.creadoEn(), d.actualizadoEn(), enviado);
    }

    /**
     * Reactiva un representante desactivado. Queda PENDIENTE si nunca ingresó y ACTIVA si ya había
     * ingresado (misma regla que el módulo de usuarios).
     */
    @Transactional
    public CuentaDTO activarRepresentante(Long id) {

        Long empresaId = obtenerEmpresaAutorizada();

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(id, empresaId, RolCuenta.REPRESENTANTE)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        if (representante.getEstado() != EstadoCuenta.INACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El representante ya está activo"
            );
        }

        representante.setEstado(
            representante.getUltimoIngresoEn() == null ? EstadoCuenta.PENDIENTE : EstadoCuenta.ACTIVA);
        representante.setActualizadoEn(OffsetDateTime.now());

        return cuentaMapper.toDto(cuentaRepository.saveAndFlush(representante));
    }

    @Transactional
    public CuentaDTO desactivarRepresentante(Long id) {

        Long empresaId = obtenerEmpresaAutorizada();

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(
                id,
                empresaId,
                RolCuenta.REPRESENTANTE
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        if (representante.getEstado() == EstadoCuenta.INACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El representante ya está desactivado"
            );
        }

        representante.setEstado(EstadoCuenta.INACTIVA);
        representante.setActualizadoEn(OffsetDateTime.now());

        Cuenta actualizada = cuentaRepository.saveAndFlush(representante);

        return cuentaMapper.toDto(actualizada);
    }

    /**
     * Empresa sobre la que se gestionan los representantes: la propia del cliente-administrador o,
     * para el administrador de la plataforma, la empresa elegida en modo soporte.
     */
    private Long obtenerEmpresaAutorizada() {

        Cuenta usuario = usuarioActualService.obtenerCuentaActual();

        if (usuario.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no está activa"
            );
        }

        if (usuario.getRol() != RolCuenta.CLIENTE_ADMINISTRADOR
            && usuario.getRol() != RolCuenta.ADMINISTRADOR) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para administrar representantes"
            );
        }

        return usuarioActualService.empresaEfectiva();
    }

    /**
     * Actualiza el nombre o correo de un representante.
     *
     * @param id identificador del representante
     * @param dto campos que se actualizarán y versión esperada de la cuenta
     * @return representante actualizado; indica si se envió la contraseña temporal cuando aplica
     */
    @Transactional
    public CuentaDTO actualizarRepresentante(Long id, ActualizarRepresentanteDTO dto) {

        Long empresaId = obtenerEmpresaAutorizada();

        if (dto == null || (dto.nombreCompleto() == null && dto.correo() == null)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe enviar al menos el nombre completo o el correo electrónico"
            );
        }

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(id, empresaId, RolCuenta.REPRESENTANTE)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        if (!Objects.equals(representante.getVersion(), dto.version())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La cuenta fue modificada. Actualice la información"
            );
        }

        boolean huboCambios = false;
        boolean cambioDireccionCorreo = false;

        if (dto.nombreCompleto() != null) {
            String nombreNormalizado = dto.nombreCompleto().trim();
            if (nombreNormalizado.length() < 2) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre debe tener al menos 2 caracteres"
                );
            }
            if (nombreNormalizado.length() > 100) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar 100 caracteres"
                );
            }
            if (!nombreNormalizado.equals(representante.getNombreCompleto())) {
                representante.setNombreCompleto(nombreNormalizado);
                huboCambios = true;
            }
        }

        if (dto.correo() != null) {
            String correoNormalizado = dto.correo().trim().toLowerCase(Locale.ROOT);
            if (correoNormalizado.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo electrónico es obligatorio"
                );
            }
            if (correoNormalizado.length() > 254) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo no puede superar 254 caracteres"
                );
            }

            cambioDireccionCorreo = !correoNormalizado.equalsIgnoreCase(representante.getCorreo());
            if (cambioDireccionCorreo) {
                if (cuentaRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
                    throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una cuenta registrada con este correo"
                    );
                }
            }
            if (!correoNormalizado.equals(representante.getCorreo())) {
                representante.setCorreo(correoNormalizado);
                huboCambios = true;
            }
        }

        if (!huboCambios) {
            return cuentaMapper.toDto(representante);
        }

        String contrasenaTemporal = null;
        if (cambioDireccionCorreo
            && representante.getEstado() == EstadoCuenta.PENDIENTE
            && representante.getGoogleSub() == null
            && representante.getContrasenaHash() != null
            && representante.isDebeCambiarContrasena()) {
            contrasenaTemporal = contrasenaTemporalService.asignar(representante);
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        representante.setActualizadoEn(ahora);
        try {
            Cuenta actualizada = cuentaRepository.saveAndFlush(representante);

            CuentaDTO resultado = cuentaMapper.toDto(actualizada);
            if (contrasenaTemporal != null) {
                boolean enviado = correoService.enviarContrasenaTemporal(
                    actualizada.getCorreo(),
                    actualizada.getNombreCompleto(),
                    contrasenaTemporal,
                    actualizada.getContrasenaTemporalVenceEn()
                );
                return conEnvio(resultado, enviado);
            }
            return resultado;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo actualizar el representante. Verifique el correo",
                ex
            );
        }
    }
}
