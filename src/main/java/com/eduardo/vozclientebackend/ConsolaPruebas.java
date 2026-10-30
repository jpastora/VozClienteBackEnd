package com.eduardo.vozclientebackend;


import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Scanner;
import java.util.UUID;

/**
 * Consola para probar los endpoints de VozCliente backend.
 * Requiere Java 11+ y no usa dependencias externas.
 *
 * Ejecutar:  java ConsolaPruebas.java [urlBase]
 * Ejemplo:   java ConsolaPruebas.java http://localhost:8080
 */
public class ConsolaPruebas {

    private static String baseUrl = "http://localhost:8070";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private static final Scanner IN = new Scanner(System.in);

    public static void main(String[] args) {
        if (args.length > 0) baseUrl = quitarBarraFinal(args[0]);

        while (true) {
            System.out.println("\n================ VOZ CLIENTE - CONSOLA DE PRUEBAS ================");
            System.out.println("URL base: " + baseUrl);
            System.out.println("1. Salas");
            System.out.println("2. Entrevistas");
            System.out.println("3. Grabaciones");
            System.out.println("4. Transcripciones");
            System.out.println("9. Cambiar URL base");
            System.out.println("0. Salir");
            String op = leer("Opción");
            try {
                switch (op) {
                    case "1" -> menuSalas();
                    case "2" -> menuEntrevistas();
                    case "3" -> menuGrabaciones();
                    case "4" -> menuTranscripciones();
                    case "9" -> baseUrl = quitarBarraFinal(leer("Nueva URL base"));
                    case "0" -> { System.out.println("¡Hasta luego!"); return; }
                    default -> System.out.println("Opción inválida.");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------- SALAS
    private static void menuSalas() throws Exception {
        System.out.println("\n--- SALAS (/api/salas) ---");
        System.out.println("1. Crear sala                  POST  /api/salas");
        System.out.println("2. Obtener por token           GET   /api/salas/{token}");
        System.out.println("3. Listar por producto         GET   /api/salas/producto/{productoId}");
        System.out.println("4. Responder sala              PATCH /api/salas/{token}/responder");
        System.out.println("0. Volver");
        switch (leer("Opción")) {
            case "1" -> {
                System.out.println("(El token, el estado y las fechas los genera el servidor)");
                String json = jsonNumerico(
                    "productoId", leer("productoId"),
                    "creadoPorId", leer("creadoPorId (Enter para omitir)"));
                enviar("POST", "/api/salas", json);
            }
            case "2" -> enviar("GET", "/api/salas/" + leer("Token"), null);
            case "3" -> enviar("GET", "/api/salas/producto/" + leer("productoId"), null);
            case "4" -> {
                String token = leer("Token");
                String nombre = URLEncoder.encode(leer("nombreConsumidor"), StandardCharsets.UTF_8);
                enviar("PATCH", "/api/salas/" + token + "/responder?nombreConsumidor=" + nombre, null);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------- ENTREVISTAS
    private static void menuEntrevistas() throws Exception {
        System.out.println("\n--- ENTREVISTAS (/api/entrevistas) ---");
        System.out.println("1. Iniciar entrevista          POST  /api/entrevistas");
        System.out.println("2. Obtener por ID              GET   /api/entrevistas/{id}");
        System.out.println("3. Listar por sala             GET   /api/entrevistas/sala/{salaId}");
        System.out.println("4. Listar por representante    GET   /api/entrevistas/representante/{id}");
        System.out.println("5. Finalizar entrevista        PATCH /api/entrevistas/{id}/finalizar");
        System.out.println("6. Reportar fallo              PATCH /api/entrevistas/{id}/fallo");
        System.out.println("0. Volver");
        switch (leer("Opción")) {
            case "1" -> {
                System.out.println("(El estado EN_CURSO y la fecha de inicio los asigna el servidor)");
                String json = jsonNumerico(
                    "salaId", leer("salaId"),
                    "representanteId", leer("representanteId"));
                enviar("POST", "/api/entrevistas", json);
            }
            case "2" -> enviar("GET", "/api/entrevistas/" + leer("ID"), null);
            case "3" -> enviar("GET", "/api/entrevistas/sala/" + leer("salaId"), null);
            case "4" -> enviar("GET", "/api/entrevistas/representante/" + leer("representanteId"), null);
            case "5" -> enviar("PATCH", "/api/entrevistas/" + leer("ID") + "/finalizar", null);
            case "6" -> enviar("PATCH", "/api/entrevistas/" + leer("ID") + "/fallo", null);
            default -> { }
        }
    }

    // ------------------------------------------------------------- GRABACIONES
    private static void menuGrabaciones() throws Exception {
        System.out.println("\n--- GRABACIONES (/api/v1/grabaciones) ---");
        System.out.println("1. Subir grabación (.webm)     POST  /api/v1/grabaciones (multipart)");
        System.out.println("2. Obtener por entrevista      GET   /api/v1/grabaciones/entrevista/{id}");
        System.out.println("0. Volver");
        switch (leer("Opción")) {
            case "1" -> subirGrabacion();
            case "2" -> enviar("GET", "/api/v1/grabaciones/entrevista/" + leer("entrevistaId"), null);
            default -> { }
        }
    }

    private static void subirGrabacion() throws Exception {
        String entrevistaId = leer("entrevistaId");
        Path archivo = Path.of(leer("Ruta del archivo .webm"));
        if (!Files.isRegularFile(archivo)) {
            System.out.println("El archivo no existe: " + archivo);
            return;
        }
        String duracion = leerConDefecto("duracionSegundos", "0");
        String audioCliente = leerConDefecto("tieneAudioCliente (true/false)", "true");
        String parcial = leerConDefecto("esParcial (true/false)", "false");

        String boundary = "----VozCliente" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        campo(out, boundary, "entrevistaId", entrevistaId);
        campo(out, boundary, "duracionSegundos", duracion);
        campo(out, boundary, "tieneAudioCliente", audioCliente);
        campo(out, boundary, "esParcial", parcial);

        String cab = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"archivo\"; filename=\""
            + archivo.getFileName() + "\"\r\n"
            + "Content-Type: audio/webm\r\n\r\n";
        out.write(cab.getBytes(StandardCharsets.UTF_8));
        out.write(Files.readAllBytes(archivo));
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/grabaciones"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .timeout(Duration.ofMinutes(2))
            .POST(BodyPublishers.ofByteArray(out.toByteArray()))
            .build();
        ejecutar(req, "POST /api/v1/grabaciones");
    }

    private static void campo(ByteArrayOutputStream out, String boundary, String nombre, String valor)
        throws Exception {
        String parte = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n"
            + valor + "\r\n";
        out.write(parte.getBytes(StandardCharsets.UTF_8));
    }

    // --------------------------------------------------------- TRANSCRIPCIONES
    private static void menuTranscripciones() throws Exception {
        System.out.println("\n--- TRANSCRIPCIONES (/api/transcripciones) ---");
        System.out.println("1. Solicitar transcripción     POST /api/transcripciones/grabacion/{id}");
        System.out.println("2. Reintentar transcripción    POST /api/transcripciones/grabacion/{id}/reintentar");
        System.out.println("3. Obtener por ID              GET  /api/transcripciones/{id}");
        System.out.println("4. Obtener por grabación       GET  /api/transcripciones/grabacion/{id}");
        System.out.println("5. Polling del estado (por grabación, cada 3s)");
        System.out.println("0. Volver");
        switch (leer("Opción")) {
            case "1" -> enviar("POST", "/api/transcripciones/grabacion/" + leer("grabacionId"), null);
            case "2" -> enviar("POST", "/api/transcripciones/grabacion/" + leer("grabacionId") + "/reintentar", null);
            case "3" -> enviar("GET", "/api/transcripciones/" + leer("ID"), null);
            case "4" -> enviar("GET", "/api/transcripciones/grabacion/" + leer("grabacionId"), null);
            case "5" -> polling(leer("grabacionId"));
            default -> { }
        }
    }

    private static void polling(String grabacionId) throws Exception {
        System.out.println("Consultando cada 3 segundos (máx. 20 intentos). Ctrl+C para cortar.");
        for (int i = 1; i <= 20; i++) {
            System.out.println("\n[Intento " + i + "]");
            HttpResponse<String> r = enviar("GET", "/api/transcripciones/grabacion/" + grabacionId, null);
            if (r != null && r.statusCode() == 200) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"estado\"\\s*:\\s*\"(\\w+)\"").matcher(r.body());
                if (m.find()) {
                    String estado = m.group(1);
                    if (estado.equals("COMPLETADA") || estado.equals("CON_ERROR")
                        || estado.equals("SIN_CONTENIDO")) {
                        System.out.println("Estado final alcanzado: " + estado);
                        return;
                    }
                }
            }
            Thread.sleep(3000);
        }
        System.out.println("Se agotaron los intentos.");
    }

    // ------------------------------------------------------------------ HTTP
    private static HttpResponse<String> enviar(String metodo, String ruta, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json");
        if (json != null) {
            b.header("Content-Type", "application/json")
                .method(metodo, BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        } else {
            b.method(metodo, BodyPublishers.noBody());
        }
        return ejecutar(b.build(), metodo + " " + ruta);
    }

    public static HttpResponse<String> ejecutar(HttpRequest req, String etiqueta) {
        try {
            System.out.println("\n>>> " + etiqueta);
            long t0 = System.currentTimeMillis();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            long ms = System.currentTimeMillis() - t0;
            System.out.println("<<< HTTP " + res.statusCode() + " (" + ms + " ms)");
            String body = res.body();
            if (body != null && !body.isBlank()) System.out.println(formatearJson(body));
            return res;
        } catch (Exception e) {
            System.out.println("No se pudo completar la petición: " + e.getMessage());
            return null;
        }
    }

    // --------------------------------------------------------------- UTILIDAD
    private static String leer(String etiqueta) {
        System.out.print(etiqueta + ": ");
        return IN.nextLine().trim();
    }

    private static String leerConDefecto(String etiqueta, String defecto) {
        String v = leer(etiqueta + " [" + defecto + "]");
        return v.isEmpty() ? defecto : v;
    }

    /** Arma un JSON con campos numéricos a partir de pares nombre, valor. Omite los valores vacíos. */
    private static String jsonNumerico(String... paresNombreValor) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < paresNombreValor.length; i += 2) {
            String valor = paresNombreValor[i + 1];
            if (valor == null || valor.isBlank()) continue;
            if (sb.length() > 1) sb.append(',');
            sb.append('"').append(paresNombreValor[i]).append("\":").append(valor.trim());
        }
        return sb.append('}').toString();
    }

    private static String quitarBarraFinal(String u) {
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    /** Indentador de JSON simple (sin librerías). Si no parece JSON, devuelve el texto tal cual. */
    private static String formatearJson(String s) {
        String t = s.trim();
        if (!(t.startsWith("{") || t.startsWith("["))) return s;
        StringBuilder sb = new StringBuilder();
        int nivel = 0;
        boolean enTexto = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '"' && (i == 0 || t.charAt(i - 1) != '\\')) enTexto = !enTexto;
            if (enTexto) { sb.append(c); continue; }
            switch (c) {
                case '{', '[' -> {
                    sb.append(c);
                    boolean vacio = i + 1 < t.length() && (t.charAt(i + 1) == '}' || t.charAt(i + 1) == ']');
                    if (!vacio) { nivel++; sb.append('\n').append("  ".repeat(nivel)); }
                }
                case '}', ']' -> {
                    boolean vacio = i > 0 && (t.charAt(i - 1) == '{' || t.charAt(i - 1) == '[');
                    if (!vacio) { nivel--; sb.append('\n').append("  ".repeat(nivel)); }
                    sb.append(c);
                }
                case ',' -> sb.append(c).append('\n').append("  ".repeat(nivel));
                case ':' -> sb.append(": ");
                case ' ', '\n', '\r', '\t' -> { }
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
