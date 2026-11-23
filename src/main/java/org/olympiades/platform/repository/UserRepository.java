package org.olympiades.platform.repository;

import java.util.UUID;

import org.olympiades.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
}
