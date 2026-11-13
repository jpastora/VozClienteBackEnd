package com.eduardo.vozclientebackend.websockets;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SignalingSocketHandler extends TextWebSocketHandler {

    // Guarda las sesiones activas (quiénes están conectados)
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Aquí recibes las ofertas y respuestas de WebRTC y se las envías al otro usuario
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && !session.getId().equals(s.getId())) {
                s.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
    }
}
