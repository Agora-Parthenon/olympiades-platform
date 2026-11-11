package org.olympiades.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.olympiades.platform.TestcontainersConfiguration;
import org.olympiades.platform.repository.ProfileRepository;
import org.olympiades.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProfileServiceTest extends TestcontainersConfiguration {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void createsTheProfileOnTheFirstVisit() {
        UUID id = UUID.randomUUID();

        var result = profileService.ensureProfile(id, "first-visit");

        assertThat(result.created()).isTrue();
        assertThat(result.profile().getUserId()).isEqualTo(id);
        assertThat(result.profile().getPseudo()).isEqualTo("first-visit");
        assertThat(userRepository.existsById(id)).isTrue();
    }

    @Test
    void returnsTheExistingProfileWithoutDuplicatingIt() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "comes-back");
        long usersBefore = userRepository.count();
        long profilesBefore = profileRepository.count();

        var again = profileService.ensureProfile(id, "comes-back");

        assertThat(again.created()).isFalse();
        assertThat(again.profile().getPseudo()).isEqualTo("comes-back");
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(profileRepository.count()).isEqualTo(profilesBefore);
    }

    @Test
    void keepsTheCurrentPseudoEvenIfKeycloakNowSaysSomethingElse() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "old-name");

        var again = profileService.ensureProfile(id, "new-name");

        assertThat(again.profile().getPseudo()).isEqualTo("old-name");
    }

    @Test
    void givesANewcomerAFreePseudoWhenTheirNameIsAlreadyTaken() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        profileService.ensureProfile(first, "popular");

        var result = profileService.ensureProfile(second, "popular");

        assertThat(result.created()).isTrue();
        assertThat(result.profile().getPseudo())
                .isNotEqualTo("popular")
                .startsWith("popular-");
    }

    @Test
    void shortensAPseudoThatIsTooLong() {
        var result = profileService.ensureProfile(UUID.randomUUID(), "x".repeat(80));

        assertThat(result.profile().getPseudo()).hasSize(ProfileService.MAX_PSEUDO_LENGTH);
    }

    @Test
    void shortensATooLongPseudoAgainWhenAddingTheSuffix() {
        String longName = "y".repeat(80);
        profileService.ensureProfile(UUID.randomUUID(), longName);

        var result = profileService.ensureProfile(UUID.randomUUID(), longName);

        assertThat(result.profile().getPseudo()).hasSizeLessThanOrEqualTo(ProfileService.MAX_PSEUDO_LENGTH);
        assertThat(profileRepository.count()).isGreaterThanOrEqualTo(2);
    }
}
