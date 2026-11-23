package org.olympiades.platform.repository;

import java.util.UUID;

import org.olympiades.platform.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    boolean existsByPseudoIgnoreCase(String pseudo);
}
