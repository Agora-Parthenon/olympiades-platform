package org.olympiades.platform.kafka;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KafkaUserEventPublisher implements UserEventPublisher {

    public static final String TOPIC = "platform.user.events";
    public static final String PSEUDO_CHANGED = "USER_PSEUDO_CHANGED";

    private static final Logger log = LoggerFactory.getLogger(KafkaUserEventPublisher.class);

    public record PseudoChangedEvent(String type, UUID userId, String oldPseudo, String newPseudo,
            Instant occurredAt) {
    }

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    public KafkaUserEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    /**
     * Best effort: the profile is already saved, so a Kafka problem is logged and never makes
     * the player's request fail. The key is the user id, so a player's events stay in order.
     */
    @Override
    public void pseudoChanged(UUID userId, String oldPseudo, String newPseudo) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new PseudoChangedEvent(PSEUDO_CHANGED, userId, oldPseudo, newPseudo, Instant.now()));
            kafka.send(TOPIC, userId.toString(), payload).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Could not publish the pseudo change of user {} on {}", userId, TOPIC, error);
                }
            });
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Could not publish the pseudo change of user {} on {}", userId, TOPIC, e);
        }
    }
}
