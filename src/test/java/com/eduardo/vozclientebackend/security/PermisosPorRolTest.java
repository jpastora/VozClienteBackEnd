package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.controllers.CompraController;
import com.eduardo.vozclientebackend.controllers.CuentaController;
import com.eduardo.vozclientebackend.controllers.EmpresaController;
import com.eduardo.vozclientebackend.controllers.EntrevistaController;
import com.eduardo.vozclientebackend.controllers.GrabacionController;
import com.eduardo.vozclientebackend.controllers.PaqueteController;
import com.eduardo.vozclientebackend.controllers.ProductoController;
import com.eduardo.vozclientebackend.controllers.RepresentanteController;
import com.eduardo.vozclientebackend.controllers.SalaController;
import com.eduardo.vozclientebackend.controllers.TranscripcionController;
import com.eduardo.vozclientebackend.enums.RolCuenta;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la matriz de permisos por rol (RF-02).
 */
class PermisosPorRolTest {

    private static final Pattern PERMISO = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    private static final List<Class<?>> CONTROLLERS = List.of(
        CompraController.class, CuentaController.class, EmpresaController.class, EntrevistaController.class,
        GrabacionController.class, PaqueteController.class, ProductoController.class,
        RepresentanteController.class, SalaController.class, TranscripcionController.class);

    /** Métodos que deben quedar sin permiso porque los usa el consumidor sin cuenta (RF-08). */
    private static final Set<String> PUBLICOS = Set.of(
        "SalaController.obtenerPorToken", "SalaController.responderSala");

    private static List<String> autoridades(RolCuenta rol) {
        return PermisosPorRol.autoridades(rol).stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void autoridades_clienteAdministrador_incluyeRolYPermisosDeSuEmpresa() {
        assertThat(autoridades(RolCuenta.CLIENTE_ADMINISTRADOR))
            .contains("ROLE_CLIENTE_ADMINISTRADOR", "producto:leer", "producto:escribir", "sala:crear",
                "representante:gestionar")
            .doesNotContain("cuenta:gestionar", "paquete:gestionar");
    }

    @Test
    void autoridades_representante_noPuedeAdministrarProductosNiRepresentantes() {
        assertThat(autoridades(RolCuenta.REPRESENTANTE))
            .contains("ROLE_REPRESENTANTE", "producto:leer", "sala:leer", "entrevista:gestionar", "grabacion:subir")
            .doesNotContain("producto:escribir", "sala:crear", "representante:gestionar", "compra:crear");
    }

    @Test
    void autoridades_administrador_gestionaCuentasYPaquetesPeroNoProductos() {
        assertThat(autoridades(RolCuenta.ADMINISTRADOR))
            .contains("ROLE_ADMINISTRADOR", "cuenta:gestionar", "paquete:gestionar", "empresa:gestionar")
            .doesNotContain("producto:leer", "producto:escribir");
    }

    @Test
    void preAuthorize_cadaPermisoDeLosControllers_existeEnLaMatriz() {
        List<String> desconocidos = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method metodo : controller.getDeclaredMethods()) {
                PreAuthorize anotacion = metodo.getAnnotation(PreAuthorize.class);
                if (anotacion == null) {
                    continue;
                }
                Matcher permiso = PERMISO.matcher(anotacion.value());
                if (!permiso.find() || !PermisosPorRol.todos().contains(permiso.group(1))) {
                    desconocidos.add(controller.getSimpleName() + "." + metodo.getName() + " → " + anotacion.value());
                }
            }
        }
        assertThat(desconocidos).as("Permisos que no existen en PermisosPorRol").isEmpty();
    }

    @Test
    void preAuthorize_todoEndpointNoPublico_exigeUnPermiso() {
        List<String> sinPermiso = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method metodo : controller.getDeclaredMethods()) {
                boolean esEndpoint = Arrays.stream(metodo.getAnnotations())
                    .anyMatch(a -> a.annotationType().isAnnotationPresent(RequestMapping.class)
                        || a.annotationType().equals(RequestMapping.class));
                String nombre = controller.getSimpleName() + "." + metodo.getName();
                if (esEndpoint && !PUBLICOS.contains(nombre) && metodo.getAnnotation(PreAuthorize.class) == null) {
                    sinPermiso.add(nombre);
                }
            }
        }
        assertThat(sinPermiso).as("Endpoints sin @PreAuthorize").isEmpty();
    }
}
