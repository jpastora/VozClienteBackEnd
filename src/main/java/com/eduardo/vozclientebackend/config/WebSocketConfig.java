package com.eduardo.vozclientebackend.config;

import com.eduardo.vozclientebackend.websockets.SignalingSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingSocketHandler signalingSocketHandler;

    public WebSocketConfig(SignalingSocketHandler signalingSocketHandler) {
        this.signalingSocketHandler = signalingSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // El frontend se conectará a ws://localhost:8070/signaling
        registry.addHandler(signalingSocketHandler, "/signaling").setAllowedOrigins("*");
    }
}
