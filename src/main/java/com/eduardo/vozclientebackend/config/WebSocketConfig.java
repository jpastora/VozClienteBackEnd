package com.eduardo.vozclientebackend.config;

import com.eduardo.vozclientebackend.websockets.SignalingSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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

    /**
     * Tomcat limita cada mensaje de texto a 8 KB y una oferta SDP con audio y video puede superarlo
     * (la conexión se cerraría con error 1009).
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        return container;
    }
}
