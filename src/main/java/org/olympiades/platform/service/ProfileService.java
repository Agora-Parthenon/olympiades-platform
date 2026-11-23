package org.olympiades.platform.service;

import java.util.UUID;

import java.util.regex.Pattern;

import org.olympiades.platform.exception.InvalidProfileException;
import org.olympiades.platform.exception.ProfileNotFoundException;
import org.olympiades.platform.exception.PseudoAlreadyUsedException;
import org.olympiades.platform.kafka.UserEventPublisher;
import org.olympiades.platform.model.Avatars;
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
    private static final Pattern PSEUDO_PATTERN = Pattern.compile("[A-Za-z0-9-]{3,20}");

    /** The profile, and whether this very call created it. */
    public record EnsuredProfile(Profile profile, boolean created) {
    }

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TransactionTemplate transaction;
    private final UserEventPublisher events;

    public ProfileService(UserRepository userRepository, ProfileRepository profileRepository,
            TransactionTemplate transaction, UserEventPublisher events) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.transaction = transaction;
        this.events = events;
    }

    public Profile getProfile(UUID userId) {
        return profileRepository.findById(userId).orElseThrow(ProfileNotFoundException::new);
    }

    /**
     * Changes the pseudo and the avatar of a player. A pseudo is 3 to 20 letters, digits or
     * hyphens, and unique whatever the case. The games are told when the pseudo changes.
     */
    public Profile updateProfile(UUID userId, String pseudo, String avatar) {
        String newPseudo = pseudo == null ? "" : pseudo.strip();
        if (!PSEUDO_PATTERN.matcher(newPseudo).matches()) {
            throw new InvalidProfileException("invalid_pseudo",
                    "The pseudo must have 3 to 20 letters, digits or hyphens");
        }
        if (avatar != null && !Avatars.isValid(avatar)) {
            throw new InvalidProfileException("invalid_avatar", "Unknown avatar: " + avatar);
        }

        String[] oldPseudo = new String[1];
        Profile saved;
        try {
            saved = transaction.execute(status -> {
                Profile profile = getProfile(userId);
                oldPseudo[0] = profile.getPseudo();
                boolean takenByAnother = !oldPseudo[0].equalsIgnoreCase(newPseudo)
                        && profileRepository.existsByPseudoIgnoreCase(newPseudo);
                if (takenByAnother) {
                    throw new PseudoAlreadyUsedException(newPseudo);
                }
                profile.update(newPseudo, avatar);
                return profileRepository.saveAndFlush(profile);
            });
        } catch (DataIntegrityViolationException e) {
            // Someone took the same pseudo between the check and the save
            throw new PseudoAlreadyUsedException(newPseudo);
        }

        if (!oldPseudo[0].equals(newPseudo)) {
            events.pseudoChanged(userId, oldPseudo[0], newPseudo);
        }
        return saved;
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
        if (!profileRepository.existsByPseudoIgnoreCase(candidate)) {
            return candidate;
        }
        String suffix = "-" + userId.toString().substring(0, SUFFIX_LENGTH);
        int baseLength = Math.min(pseudo.length(), MAX_PSEUDO_LENGTH - suffix.length());
        return pseudo.substring(0, baseLength) + suffix;
    }
}
