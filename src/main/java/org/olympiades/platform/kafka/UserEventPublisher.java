package org.olympiades.platform.kafka;

import java.util.UUID;

/** Tells the other services (the games) about what happens to the players. */
public interface UserEventPublisher {

    void pseudoChanged(UUID userId, String oldPseudo, String newPseudo);
}
