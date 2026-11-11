package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.enums.RolCuenta;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Matriz de permisos por rol (RF-02, RNF-01): único lugar donde se decide qué puede hacer cada rol.
 *
 * <p>Cada endpoint declara el permiso que exige con
 * {@code @PreAuthorize("hasAuthority('producto:leer')")}. Al iniciar sesión, el filtro de la cookie
 * entrega a la persona su rol ({@code ROLE_...}) y los permisos de este mapa. Como el rol se lee de
 * la base de datos en cada solicitud, un cambio de rol aplica desde la siguiente solicitud (RF-02).</p>
 *
 * <p>Los perfiles siguen la Tabla 5 del informe de diseño (pantallas que usa cada perfil).</p>
 */
public final class PermisosPorRol {

    // ---- Catálogo de permisos (formato recurso:accion) ----

    /** Consultar productos de la empresa. */
    public static final String PRODUCTO_LEER = "producto:leer";
    /** Crear, editar, inactivar y eliminar productos. */
    public static final String PRODUCTO_ESCRIBIR = "producto:escribir";
    /** Consultar las salas de un producto. */
    public static final String SALA_LEER = "sala:leer";
    /** Generar enlaces de sala. */
    public static final String SALA_CREAR = "sala:crear";
    /** Crear, consultar y desactivar representantes. */
    public static final String REPRESENTANTE_GESTIONAR = "representante:gestionar";
    /** Consultar entrevistas. */
    public static final String ENTREVISTA_LEER = "entrevista:leer";
    /** Iniciar, finalizar y reportar fallas de entrevistas. */
    public static final String ENTREVISTA_GESTIONAR = "entrevista:gestionar";
    /** Consultar grabaciones. */
    public static final String GRABACION_LEER = "grabacion:leer";
    /** Subir la grabación de una entrevista. */
    public static final String GRABACION_SUBIR = "grabacion:subir";
    /** Consultar transcripciones. */
    public static final String TRANSCRIPCION_LEER = "transcripcion:leer";
    /** Solicitar o reintentar transcripciones. */
    public static final String TRANSCRIPCION_GESTIONAR = "transcripcion:gestionar";
    /** Consultar el catálogo de paquetes. */
    public static final String PAQUETE_LEER = "paquete:leer";
    /** Crear, editar y desactivar paquetes (RF-17). */
    public static final String PAQUETE_GESTIONAR = "paquete:gestionar";
    /** Consultar compras. */
    public static final String COMPRA_LEER = "compra:leer";
    /** Comprar un paquete (RF-18). */
    public static final String COMPRA_CREAR = "compra:crear";
    /** Eliminar compras (administración de la plataforma). */
    public static final String COMPRA_GESTIONAR = "compra:gestionar";
    /** Administrar cuentas y roles (RF-02). */
    public static final String CUENTA_GESTIONAR = "cuenta:gestionar";
    /** Administrar empresas. */
    public static final String EMPRESA_GESTIONAR = "empresa:gestionar";

    public static final String GUIA_LEER = "guia:leer";

    public static final String GUIA_CREAR = "guia:crear";

    public static final String GUIA_GESTIONAR = "guia:gestionar";

    private static final Map<RolCuenta, Set<String>> PERMISOS = new EnumMap<>(RolCuenta.class);

    static {
        PERMISOS.put(RolCuenta.CLIENTE_ADMINISTRADOR, Set.of(
            PRODUCTO_LEER, PRODUCTO_ESCRIBIR,
            SALA_LEER, SALA_CREAR,
            REPRESENTANTE_GESTIONAR,
            ENTREVISTA_LEER, ENTREVISTA_GESTIONAR,
            GRABACION_LEER, GRABACION_SUBIR,
            TRANSCRIPCION_LEER, TRANSCRIPCION_GESTIONAR,
            PAQUETE_LEER,
            COMPRA_LEER, COMPRA_CREAR, GUIA_LEER, GUIA_CREAR, GUIA_GESTIONAR));

        PERMISOS.put(RolCuenta.REPRESENTANTE, Set.of(
            PRODUCTO_LEER,
            SALA_LEER, SALA_CREAR,
            ENTREVISTA_LEER, ENTREVISTA_GESTIONAR, GUIA_LEER,
            GRABACION_SUBIR));

        PERMISOS.put(RolCuenta.ADMINISTRADOR, Set.of(
            CUENTA_GESTIONAR,
            EMPRESA_GESTIONAR,
            PAQUETE_LEER, PAQUETE_GESTIONAR,
            COMPRA_LEER, COMPRA_GESTIONAR));
    }

    private PermisosPorRol() {
    }

    /**
     * Permisos de un rol.
     *
     * @param rol rol de la cuenta
     * @return conjunto inmutable de permisos; vacío si el rol no tiene permisos
     */
    public static Set<String> permisos(RolCuenta rol) {
        return PERMISOS.getOrDefault(rol, Set.of());
    }

    /**
     * Todos los permisos del catálogo, asignados a algún rol.
     *
     * @return conjunto ordenado e inmutable de permisos
     */
    public static Set<String> todos() {
        Set<String> todos = new TreeSet<>();
        PERMISOS.values().forEach(todos::addAll);
        return Collections.unmodifiableSet(todos);
    }

    /**
     * Autoridades de Spring Security para un rol: {@code ROLE_<rol>} más sus permisos.
     *
     * @param rol rol de la cuenta
     * @return lista de autoridades
     */
    public static List<GrantedAuthority> autoridades(RolCuenta rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        new TreeSet<>(permisos(rol)).forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));
        return autoridades;
    }
}
