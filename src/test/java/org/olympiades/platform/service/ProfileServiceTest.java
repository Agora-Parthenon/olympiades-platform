package org.olympiades.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.olympiades.platform.exception.InvalidProfileException;
import org.olympiades.platform.exception.ProfileNotFoundException;
import org.olympiades.platform.exception.PseudoAlreadyUsedException;
import org.olympiades.platform.TestcontainersConfiguration;
import org.olympiades.platform.repository.ProfileRepository;
import org.olympiades.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.olympiades.platform.kafka.UserEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ProfileServiceTest extends TestcontainersConfiguration {

    @MockitoBean
    private UserEventPublisher events;

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

    @Test
    void updatesThePseudoAndTheAvatarAndTellsTheGames() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "before-update");

        var updated = profileService.updateProfile(id, "after-update", "fox");

        assertThat(updated.getPseudo()).isEqualTo("after-update");
        assertThat(updated.getAvatar()).isEqualTo("fox");
        assertThat(profileService.getProfile(id).getPseudo()).isEqualTo("after-update");
        verify(events).pseudoChanged(id, "before-update", "after-update");
    }

    @Test
    void doesNotTellTheGamesWhenOnlyTheAvatarChanges() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "same-pseudo");

        profileService.updateProfile(id, "same-pseudo", "owl");

        verify(events, never()).pseudoChanged(any(), any(), any());
    }

    @Test
    void refusesAPseudoUsedByAnotherPlayerWhateverTheCase() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        profileService.ensureProfile(owner, "TakenName");
        profileService.ensureProfile(other, "other-player");

        assertThatThrownBy(() -> profileService.updateProfile(other, "takenname", null))
                .isInstanceOf(PseudoAlreadyUsedException.class);

        assertThat(profileService.getProfile(other).getPseudo()).isEqualTo("other-player");
        verify(events, never()).pseudoChanged(any(), any(), any());
    }

    @Test
    void letsAPlayerChangeTheCaseOfTheirOwnPseudo() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "mixedcase");

        var updated = profileService.updateProfile(id, "MixedCase", null);

        assertThat(updated.getPseudo()).isEqualTo("MixedCase");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "this-pseudo-is-way-too-long", "with space", "accentué", "<script>", ""})
    void refusesAPseudoThatBreaksTheRules(String pseudo) {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "valid-pseudo");

        assertThatThrownBy(() -> profileService.updateProfile(id, pseudo, null))
                .isInstanceOfSatisfying(InvalidProfileException.class,
                        e -> assertThat(e.getCode()).isEqualTo("invalid_pseudo"));
    }

    @Test
    void refusesAnUnknownAvatar() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "avatar-test");

        assertThatThrownBy(() -> profileService.updateProfile(id, "avatar-test", "../../etc/passwd"))
                .isInstanceOfSatisfying(InvalidProfileException.class,
                        e -> assertThat(e.getCode()).isEqualTo("invalid_avatar"));
    }

    @Test
    void refusesToUpdateAPlayerWithoutProfile() {
        assertThatThrownBy(() -> profileService.updateProfile(UUID.randomUUID(), "nobody", null))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void rejectsANullPseudo() {
        UUID id = UUID.randomUUID();
        profileService.ensureProfile(id, "null-test");

        assertThatThrownBy(() -> profileService.updateProfile(id, null, null))
                .isInstanceOf(InvalidProfileException.class);
    }
}
