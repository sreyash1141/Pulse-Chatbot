package com.chat.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP configuration.
 *
 * Topic layout:
 *   /topic/room/{roomId}   – room broadcast channel
 *
 * Client sends to:
 *   /app/chat.join         – join a room
 *   /app/chat.send         – send a message
 *   /app/chat.leave        – leave a room
 *   /app/chat.typing       – typing indicator
 *
 * Private messages to a specific session:
 *   /queue/user            – server → single client
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker for topic (room broadcasts) and queue (private messages)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages handled by @MessageMapping controllers
        config.setApplicationDestinationPrefixes("/app");
        // Enables /user/{sessionId}/queue/... routing
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")   // tighten in production
                .withSockJS();
    }
}
