package org.olympiades.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint STOMP minimal — accepte la connexion, sans aucune logique métier pour
 * l'instant. Sert à valider que le flux front -> gateway -> platform fonctionne
 * de bout en bout (CA-02 côté front, olympiades-front). Les canaux
 * /topic/game/{lobbyId} et /app/game/{lobbyId}/action viendront avec les US
 * du lobby/jeu.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // La gateway relaie la requête d'upgrade telle quelle (même path, même
        // query string) — l'endpoint doit donc être exactement "/ws" ici aussi.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}