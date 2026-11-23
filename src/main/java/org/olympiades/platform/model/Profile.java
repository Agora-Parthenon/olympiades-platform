package org.olympiades.platform.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** What other players see of a user. */
@Entity
@Table(name = "profiles", schema = "platform")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, unique = true, length = 50)
    private String pseudo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 30)
    private String avatar;

    protected Profile() {
    }

    public Profile(UUID userId, String pseudo) {
        this.userId = userId;
        this.pseudo = pseudo;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getAvatar() {
        return avatar;
    }

    public void update(String pseudo, String avatar) {
        this.pseudo = pseudo;
        this.avatar = avatar;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
