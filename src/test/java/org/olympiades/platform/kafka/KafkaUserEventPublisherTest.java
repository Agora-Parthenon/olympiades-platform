package org.olympiades.platform.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class KafkaUserEventPublisherTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private final KafkaUserEventPublisher publisher = new KafkaUserEventPublisher(kafka, objectMapper);

    @Test
    void publishesThePseudoChangeOnTheUserTopicKeyedByTheUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publisher.pseudoChanged(id, "old-name", "new-name");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("platform.user.events"), eq(id.toString()), payload.capture());
        JsonNode event = objectMapper.readTree(payload.getValue());
        assertThat(event.get("type").asText()).isEqualTo("USER_PSEUDO_CHANGED");
        assertThat(event.get("userId").asText()).isEqualTo(id.toString());
        assertThat(event.get("oldPseudo").asText()).isEqualTo("old-name");
        assertThat(event.get("newPseudo").asText()).isEqualTo("new-name");
        assertThat(event.get("occurredAt").asText()).isNotBlank();
    }

    @Test
    void doesNotFailWhenKafkaRefusesTheMessage() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThatCode(() -> publisher.pseudoChanged(UUID.randomUUID(), "a-name", "b-name"))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotFailWhenKafkaIsUnreachable() {
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("no metadata"));

        assertThatCode(() -> publisher.pseudoChanged(UUID.randomUUID(), "a-name", "b-name"))
                .doesNotThrowAnyException();
    }
}
