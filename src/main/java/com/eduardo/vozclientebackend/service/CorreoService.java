package com.eduardo.vozclientebackend.service;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envío de correos de la plataforma.
 *
 * <p>En desarrollo el servidor SMTP es Mailpit (los correos no salen a internet; se ven en
 * http://localhost:8025). En producción se configura Gmail u otro proveedor con las variables
 * {@code MAIL_*} (ver {@code docker/.env.example}).</p>
 *
 * <p>Un fallo de envío no se propaga: el método devuelve {@code false} para que quien lo llama
 * decida (la cuenta se crea igual y el administrador puede reenviar la contraseña temporal).</p>
 */
@Service
public class CorreoService {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoService.class);
    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-CR"));

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String urlAplicacion;

    public CorreoService(JavaMailSender mailSender,
                         @Value("${app.correo.remitente:vozclientebrightcodelab@gmail.com}") String remitente,
                         @Value("${app.frontend-url:http://localhost:4200}") String urlAplicacion) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlAplicacion = urlAplicacion;
    }

    /**
     * Envía la contraseña temporal a una cuenta recién creada o a la que se le reenvió.
     *
     * @return {@code true} si el servidor de correo aceptó el mensaje
     */
    public boolean enviarContrasenaTemporal(String destino, String nombre, String contrasenaTemporal,
                                            OffsetDateTime venceEn) {
        String vence = FORMATO_FECHA.format(venceEn.atZoneSameInstant(ZoneId.of("America/Costa_Rica")));
        String texto = """
            Hola %s,

            Se creó tu cuenta en VozCliente. Para ingresar usa:

              Correo: %s
              Contraseña temporal: %s

            Ingresa en %s. Al entrar por primera vez el sistema te pedirá elegir una contraseña nueva.
            La contraseña temporal vence el %s (hora de Costa Rica); si vence, pide que te la reenvíen.

            Si tu correo es de Google también puedes ingresar con el botón "Continuar con Google".

            Si no esperabas este mensaje, ignóralo.
            """.formatted(nombre, destino, contrasenaTemporal, urlAplicacion, vence);
        return enviar(destino, "Tu acceso a VozCliente", texto);
    }

    /**
     * Envía una contraseña temporal a quien pidió recuperar su acceso ("¿Olvidó su contraseña?").
     *
     * @return {@code true} si el servidor de correo aceptó el mensaje
     */
    public boolean enviarRecuperacionContrasena(String destino, String nombre, String contrasenaTemporal,
                                                OffsetDateTime venceEn) {
        String vence = FORMATO_FECHA.format(venceEn.atZoneSameInstant(ZoneId.of("America/Costa_Rica")));
        String texto = """
            Hola %s,

            Recibimos una solicitud para recuperar el acceso a tu cuenta de VozCliente. Para ingresar usa:

              Correo: %s
              Contraseña temporal: %s

            Ingresa en %s. Al entrar, el sistema te pedirá elegir una contraseña nueva.
            La contraseña temporal vence el %s (hora de Costa Rica).

            Si no pediste este cambio, ingresa con esta contraseña temporal y elige una nueva, o avisa
            al administrador de tu empresa.
            """.formatted(nombre, destino, contrasenaTemporal, urlAplicacion, vence);
        return enviar(destino, "Recupera tu acceso a VozCliente", texto);
    }

    private boolean enviar(String destino, String asunto, String texto) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(texto, false);
            mailSender.send(mensaje);
            return true;
        } catch (Exception ex) {
            // No se registra el contenido del mensaje: lleva una contraseña.
            LOG.warn("No se pudo enviar el correo a {}: {}", destino, ex.getMessage());
            return false;
        }
    }
}
