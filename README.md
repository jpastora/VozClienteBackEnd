# Voz Cliente — Backend

API REST de **Voz Cliente** (vozcliente.com), plataforma de videollamadas para capturar y analizar con IA la voz de clientes entrevistados sobre los productos de una empresa.

Proyecto de Ingeniería del Software 3 · Universidad Cenfotec · C2 2026 · **Brightcode Labs**

---

## Tabla de contenidos

1. [Stack tecnológico](#1-stack-tecnológico)
2. [Equipo](#2-equipo)
3. [Estrategia de ramas](#3-estrategia-de-ramas)
4. [Reglas del repositorio](#4-reglas-del-repositorio)
5. [Estructura del proyecto](#5-estructura-del-proyecto)
6. [Estándar de codificación Java](#6-estándar-de-codificación-java)
7. [Estándar de base de datos PostgreSQL](#7-estándar-de-base-de-datos-postgresql)
8. [Pruebas unitarias](#8-pruebas-unitarias)
9. [Seguridad y configuración](#9-seguridad-y-configuración)
10. [Versionado](#10-versionado)

---

## 1. Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2, Spring Security (OAuth de Google) |
| Base de datos | PostgreSQL |
| Migraciones | Flyway |
| Dependencias | Maven |
| Pruebas | JUnit 5, Mockito, Spring Boot Test, JaCoCo |
| Contenedores | Docker y Docker Compose |
| Integración continua | GitHub Actions |
| Gestión del proyecto | Asana |

> Todos los integrantes deben usar las mismas versiones de Java, Maven y Docker para evitar diferencias entre ambientes (riesgo R-C03).

## 2. Equipo

| Integrante | Rol | Rama personal |
|---|---|---|
| Joseph Pastora Ramos | Coordinador general | `joseph` |
| Eduardo Chacón Zamora | Coordinador de desarrollo | `eduardo` |
| Juan Diego González Víquez | Coordinador de desarrollo | `juandiego` |
| Matías Lutz | Coordinador de calidad | `matias` |
| Trayce González Brenes | Coordinadora de soporte | `trayce` |

## 3. Estrategia de ramas

| Rama | Propósito | Ambiente |
|---|---|---|
| `main` | Versión estable para entregas y demostraciones | Producción |
| `qa` | Pruebas funcionales, de regresión y de rendimiento | Calidad |
| `dev` | Integración del trabajo del equipo | Desarrollo |
| `<nombre>` | Rama personal de trabajo de cada integrante | Desarrollo (local) |
| `hotfix/VC-###-descripcion` | Corrección urgente de un error crítico en `main` | — |

**Flujo:** `rama personal → dev → qa → main`

1. Cada integrante trabaja en su rama personal y la sincroniza con `dev` al iniciar cada jornada:
   ```bash
   git checkout <nombre>
   git pull origin dev
   ```
2. Opcionalmente, para tareas grandes, se crean ramas de funcionalidad desde la rama personal: `feature/VC-123-descripcion` o `bugfix/VC-124-descripcion`, donde `VC-123` es el identificador de la tarea en Asana.
3. Los cambios se integran a `dev` mediante pull request.
4. La Coordinadora de soporte integra `dev → qa` al cierre del sprint y `qa → main` cuando la Coordinación de calidad aprueba.
5. Los `hotfix` salen de `main` y se sincronizan de vuelta con `dev`.

## 4. Reglas del repositorio

- **Prohibido el commit directo** a `dev`, `qa` y `main`; todo cambio entra por pull request.
- **Mensajes de commit** con Conventional Commits:
  ```
  tipo(alcance): descripción [VC-123]
  ```
  Tipos permitidos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`.
  Ejemplo: `feat(sala): generar enlace único por producto [VC-042]`
- **Frecuencia:** al menos un commit por jornada y `push` al finalizarla; ningún trabajo permanece solo en una máquina.
- **Pull request:** vinculado a una historia de usuario, de tamaño reducido, con al menos una aprobación de la Coordinación de desarrollo y la integración continua en verde.
- **Merges a ramas principales:** solo la Coordinadora de soporte, quien resuelve los conflictos con apoyo del autor.
- **Qué se sube:** código fuente, pruebas, migraciones, documentación y configuración sin secretos.
- **Qué no se sube:** credenciales, archivos `.env`, grabaciones reales, dependencias descargadas (`target/`) ni archivos generados.

## 5. Estructura del proyecto

Un paquete por módulo y, dentro de cada módulo, un paquete por capa:

```
src/
├── main/
│   ├── java/com/brightcodelabs/vozcliente/
│   │   ├── config/                  # Configuración global (seguridad, CORS, etc.)
│   │   ├── producto/
│   │   │   ├── controller/          # ProductoController
│   │   │   ├── service/             # ProductoService
│   │   │   ├── repository/          # ProductoRepository
│   │   │   ├── dto/                 # ProductoRequestDto, ProductoResponseDto
│   │   │   └── entity/              # Producto
│   │   ├── sala/
│   │   └── transcripcion/
│   └── resources/
│       ├── application.yml
│       └── db/migration/            # Migraciones Flyway
└── test/java/com/brightcodelabs/vozcliente/
```

## 6. Estándar de codificación Java

### 6.1 Idioma

- Identificadores del **dominio del negocio en español sin tildes**: `producto`, `sala`, `transcripcion`.
- **Términos técnicos en inglés**: `Controller`, `Service`, `Repository`, `Dto`.
- Documentación y comentarios en español.

### 6.2 Nomenclatura

| Elemento | Convención | Ejemplo |
|---|---|---|
| Paquetes | Minúsculas, por módulo | `com.brightcodelabs.vozcliente.producto` |
| Clases e interfaces | PascalCase con sufijo de capa | `ProductoService`, `SalaController` |
| Métodos y variables | camelCase, verbo en infinitivo o presente | `generarEnlaceSala`, `totalSalas` |
| Constantes | MAYUSCULAS_CON_GUION_BAJO | `MAX_SALAS_POR_PAQUETE` |
| Capas | `controller`, `service`, `repository`, `dto`, `entity`, `config` | Un paquete por capa dentro de cada módulo |
| Endpoints REST | Sustantivos en plural, minúsculas, versionados | `/api/v1/productos` |
| Excepciones | PascalCase con sufijo `Exception` | `ProductoNoEncontradoException` |

### 6.3 Formato

- Sangría de **4 espacios** (sin tabulaciones).
- Llaves **siempre**, incluso en bloques de una línea.
- Líneas de hasta **120 caracteres**.
- Una clase pública por archivo.
- Inyección de dependencias por constructor; no usar `@Autowired` en atributos.
- Los controladores solo reciben y responden; la lógica de negocio vive en los servicios.
- Las entidades nunca se exponen directamente en la API; se usan DTO.

### 6.4 API REST

| Operación | Método | Ruta | Respuesta exitosa |
|---|---|---|---|
| Listar | `GET` | `/api/v1/productos` | `200 OK` |
| Consultar | `GET` | `/api/v1/productos/{id}` | `200 OK` |
| Crear | `POST` | `/api/v1/productos` | `201 Created` |
| Actualizar | `PUT` | `/api/v1/productos/{id}` | `200 OK` |
| Eliminar | `DELETE` | `/api/v1/productos/{id}` | `204 No Content` |

Los errores se manejan de forma centralizada con `@RestControllerAdvice` y devuelven un cuerpo uniforme.

### 6.5 Documentación con Javadoc

Obligatoria en clases y métodos públicos, con descripción, `@param`, `@return` y `@throws`:

```java
/**
 * Genera el enlace único de la sala de un producto.
 *
 * @param productoId identificador del producto
 * @return enlace de la sala
 * @throws ProductoNoEncontradoException si el producto no existe
 */
public String generarEnlaceSala(Long productoId) {
    // ...
}
```

## 7. Estándar de base de datos PostgreSQL

| Objeto | Convención | Ejemplo |
|---|---|---|
| Tablas | snake_case, en plural | `productos`, `salas` |
| Columnas | snake_case, en singular | `nombre`, `creado_en` |
| Llave primaria | Columna `id`; restricción `pk_tabla` | `pk_productos` |
| Llave foránea | Columna `tabla_singular_id`; restricción `fk_origen_destino` | `producto_id`, `fk_salas_productos` |
| Índices y únicos | `idx_tabla_columna` y `uq_tabla_columna` | `idx_salas_producto_id` |
| Booleanos | Prefijo `es_` o `tiene_` | `es_activo` |
| Fechas de auditoría | `creado_en` y `actualizado_en` | `creado_en` |
| Migraciones | Flyway `V{n}__descripcion.sql` | `V3__crear_tabla_salas.sql` |
| Documentación | `COMMENT ON` obligatorio en cada tabla y columna | Ver ejemplo |

```sql
COMMENT ON TABLE salas IS 'Salas de videollamada generadas por producto';
COMMENT ON COLUMN salas.producto_id IS 'Producto al que pertenece la sala';
```

Una migración ya integrada a `dev` **nunca se modifica**; cualquier cambio se hace con una migración nueva.

## 8. Pruebas unitarias

- Herramientas: JUnit 5, Mockito, Spring Boot Test y JaCoCo.
- Cobertura mínima: **70 % en la capa de servicios**.
- Se escriben junto con el código, dentro de la misma historia de usuario.
- Nombre de las pruebas: `metodo_condicion_resultadoEsperado`.

```java
@Test
void generarEnlaceSala_productoInexistente_lanzaExcepcion() {
    // ...
}
```

Ejecución:

```bash
./mvnw test
./mvnw verify   # incluye el reporte de cobertura de JaCoCo
```

## 9. Seguridad y configuración

- Las credenciales (Google OAuth, proveedor de IA, base de datos) se manejan **solo con variables de entorno**.
- El archivo `.env` está excluido en `.gitignore`; se versiona únicamente `.env.example` sin valores reales.
- Si se expone una clave por error, se revoca y se regenera de inmediato (riesgo R-C04).

### 9.1 Ingreso con Google (HU-01)

1. Copie `docker/.env.example` como `docker/.env` y complete `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (los entrega la Coordinadora de soporte) y `JWT_SECRET` (`openssl rand -base64 48`).
2. Solo pueden ingresar los correos registrados como **usuarios de prueba** en el proyecto de Google Cloud; pida que agreguen el suyo.
3. El ingreso empieza en `/oauth2/authorization/google`. Al volver de Google, el back-end crea la empresa y la cuenta (primer ingreso) o vincula la existente, y guarda un JWT propio en la cookie HttpOnly `VC_SESION`.
4. La sesión expira tras `APP_MINUTOS_INACTIVIDAD` minutos sin actividad (30 por defecto) y se cierra con `POST /api/auth/salir`.
5. `GET /api/auth/yo` devuelve la persona con sesión; toda ruta `/api/**` responde `401` sin sesión, excepto las del consumidor (`GET /api/salas/{token}`, `PATCH /api/salas/{token}/responder`) y `/signaling`.
6. Para probar endpoints con Postman: ingrese en el navegador, copie el valor de la cookie `VC_SESION` y envíelo como cookie en la solicitud.

### 9.2 Permisos por rol (RF-02)

- La matriz vive en `security/PermisosPorRol.java`: es el único lugar donde se decide qué puede hacer cada rol.
- Cada endpoint exige su permiso con `@PreAuthorize("hasAuthority('recurso:accion')")`; sin permiso responde `403`.
- Al agregar un endpoint nuevo: use un permiso existente o agréguelo a la matriz. `PermisosPorRolTest` falla si un endpoint queda sin `@PreAuthorize` o usa un permiso que no existe.
- Solo son públicos los endpoints del consumidor: `GET /api/salas/{token}` y `PATCH /api/salas/{token}/responder`.

URI de redirección registrados en Google Cloud: `http://localhost:8070/login/oauth2/code/google` y `http://localhost:4200/login/oauth2/code/google` (este último cuando se entra por el proxy de Angular).

## 10. Versionado

- Versionado semántico `MAYOR.MENOR.PARCHE`, siempre sobre `main`.
- Primera entrega: `v0.1.0`. Entrega final: `v1.0.0`.
- Al cierre de cada sprint se crea una etiqueta.

---

© 2026 Brightcode Labs · Universidad Cenfotec
