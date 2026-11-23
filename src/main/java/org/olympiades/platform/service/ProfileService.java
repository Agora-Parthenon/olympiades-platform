package org.olympiades.platform.service;

import java.util.UUID;

import org.olympiades.platform.model.Profile;
import org.olympiades.platform.model.User;
import org.olympiades.platform.repository.ProfileRepository;
import org.olympiades.platform.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProfileService {

    static final int MAX_PSEUDO_LENGTH = 50;
    private static final int SUFFIX_LENGTH = 6;

    /** The profile, and whether this very call created it. */
    public record EnsuredProfile(Profile profile, boolean created) {
    }

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TransactionTemplate transaction;

    public ProfileService(UserRepository userRepository, ProfileRepository profileRepository,
            TransactionTemplate transaction) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.transaction = transaction;
    }

    /**
     * Returns the profile of the given user, creating it (and the user) on their first visit.
     * Safe to call again and again: an existing profile is returned as it is.
     */
    public EnsuredProfile ensureProfile(UUID userId, String initialPseudo) {
        var existing = profileRepository.findById(userId);
        if (existing.isPresent()) {
            return new EnsuredProfile(existing.get(), false);
        }

        String pseudo = availablePseudo(userId, initialPseudo.strip());
        try {
            Profile created = transaction.execute(status -> {
                userRepository.save(new User(userId));
                return profileRepository.save(new Profile(userId, pseudo));
            });
            return new EnsuredProfile(created, true);
        } catch (DataIntegrityViolationException e) {
            // Two first requests raced and the other one won: its profile is the one to return.
            return profileRepository.findById(userId)
                    .map(profile -> new EnsuredProfile(profile, false))
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Keycloak usernames are unique, but a player may since have renamed themselves to
     * the name a newcomer registers with: the newcomer must still get a profile.
     */
    private String availablePseudo(UUID userId, String pseudo) {
        String candidate = pseudo.length() > MAX_PSEUDO_LENGTH ? pseudo.substring(0, MAX_PSEUDO_LENGTH) : pseudo;
        if (!profileRepository.existsByPseudo(candidate)) {
            return candidate;
        }
        String suffix = "-" + userId.toString().substring(0, SUFFIX_LENGTH);
        int baseLength = Math.min(pseudo.length(), MAX_PSEUDO_LENGTH - suffix.length());
        return pseudo.substring(0, baseLength) + suffix;
    }
}
