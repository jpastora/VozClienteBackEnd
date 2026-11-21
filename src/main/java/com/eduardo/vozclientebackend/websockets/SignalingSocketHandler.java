package com.eduardo.vozclientebackend.websockets;

import com.eduardo.vozclientebackend.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Señalización WebRTC por salas: cada mensaje se reenvía solo a la otra persona de la MISMA sala
 * (identificada por el token del enlace) y cada sala admite como máximo 2 participantes
 * (representante + consumidor). Una tercera conexión se rechaza con el código 1008 y el motivo
 * "La sala ya está llena.", que el front muestra en pantalla.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingSocketHandler extends TextWebSocketHandler {

    private static final int MAX_PARTICIPANTES = 2; // representante + consumidor
    private static final String ATTR_TOKEN = "token";
    private static final String ATTR_ULTIMA_ACTIVIDAD = "ultimaActividad";

    /**
     * El front envía "ping" cada 25 s. Una sesión sin actividad más tiempo que este es una conexión
     * muerta (caída de red sin cierre limpio): se expulsa para que la persona pueda reconectarse
     * en su lugar y no reciba un falso "sala llena".
     */
    private static final long INACTIVIDAD_MAX_MS = 60_000;

    private final SalaRepository salaRepository;

    // token de sala -> sesiones conectadas a esa sala
    private final Map<String, Set<WebSocketSession>> salas = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String token = session.getUri() == null ? null
            : UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("token");

        if (token == null || token.isBlank() || !salaRepository.existsByToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Sala no encontrada."));
            return;
        }

        Set<WebSocketSession> sala = salas.computeIfAbsent(token, t -> ConcurrentHashMap.newKeySet());
        // El conteo y el alta van juntos: dos conexiones simultáneas no pueden colarse ambas.
        synchronized (sala) {
            depurarMuertas(sala);
            if (sala.size() >= MAX_PARTICIPANTES) {
                log.info("Conexión rechazada: la sala ya tiene {} participantes", sala.size());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("La sala ya está llena."));
                return;
            }
            session.getAttributes().put(ATTR_TOKEN, token);
            session.getAttributes().put(ATTR_ULTIMA_ACTIVIDAD, System.currentTimeMillis());
            sala.add(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String token = (String) session.getAttributes().get(ATTR_TOKEN);
        if (token == null) return; // sesión rechazada o aún sin sala

        session.getAttributes().put(ATTR_ULTIMA_ACTIVIDAD, System.currentTimeMillis());

        // "ping" es el keep-alive del frontend (evita que nginx/Docker cierre sockets inactivos).
        if ("ping".equals(message.getPayload())) return;

        for (WebSocketSession otra : salas.getOrDefault(token, Set.of())) {
            if (otra.isOpen() && !otra.getId().equals(session.getId())) {
                // WebSocketSession no admite envíos concurrentes.
                synchronized (otra) {
                    otra.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String token = (String) session.getAttributes().get(ATTR_TOKEN);
        if (token == null) return; // las rechazadas nunca entraron en la sala

        Set<WebSocketSession> sala = salas.get(token);
        if (sala != null) {
            synchronized (sala) {
                sala.remove(session);
                if (sala.isEmpty()) salas.remove(token, sala);
            }
        }
    }

    /** Quita las sesiones cerradas y expulsa las que llevan demasiado sin dar señales de vida. */
    private void depurarMuertas(Set<WebSocketSession> sala) {
        long ahora = System.currentTimeMillis();
        for (WebSocketSession s : sala) {
            Object ultima = s.getAttributes().get(ATTR_ULTIMA_ACTIVIDAD);
            boolean inactiva = ultima instanceof Long l && ahora - l > INACTIVIDAD_MAX_MS;
            if (!s.isOpen() || inactiva) {
                sala.remove(s);
                if (s.isOpen()) {
                    try {
                        s.close(CloseStatus.GOING_AWAY.withReason("Sesión inactiva."));
                    } catch (IOException e) {
                        log.debug("No se pudo cerrar la sesión inactiva {}", s.getId());
                    }
                }
            }
        }
    }
}
