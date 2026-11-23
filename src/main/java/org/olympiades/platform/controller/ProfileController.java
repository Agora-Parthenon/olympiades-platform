package org.olympiades.platform.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.olympiades.platform.model.Avatars;
import org.olympiades.platform.model.Profile;
import org.olympiades.platform.service.ProfileService;
import org.olympiades.platform.service.ProfileService.EnsuredProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ControllerPaths.PROFILES)
public class ProfileController {

    public record ProfileResponse(UUID id, String pseudo, String avatar, Instant createdAt) {
        static ProfileResponse of(Profile profile) {
            return new ProfileResponse(profile.getUserId(), profile.getPseudo(), profile.getAvatar(),
                    profile.getCreatedAt());
        }
    }

    /** What a player can change on their profile. A missing avatar means "no avatar". */
    public record UpdateProfileRequest(String pseudo, String avatar) {
    }

    @GetMapping(ControllerPaths.ME)
    public ProfileResponse myProfile(@RequestHeader(ControllerPaths.USER_ID_HEADER) UUID userId) {
        return ProfileResponse.of(profileService.getProfile(userId));
    }

    @PutMapping(ControllerPaths.ME)
    public ProfileResponse updateMyProfile(@RequestHeader(ControllerPaths.USER_ID_HEADER) UUID userId,
            @RequestBody UpdateProfileRequest request) {
        return ProfileResponse.of(profileService.updateProfile(userId, request.pseudo(), request.avatar()));
    }

    @GetMapping(ControllerPaths.AVATARS)
    public List<String> avatars() {
        return Avatars.ALL;
    }

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Called by the front after each login. The identity comes from headers set by the gateway
     * from the verified token: 201 when the profile has just been created, 200 when it already existed.
     */
    @PostMapping(ControllerPaths.ME)
    public ResponseEntity<ProfileResponse> ensureMyProfile(
            @RequestHeader(ControllerPaths.USER_ID_HEADER) UUID userId,
            @RequestHeader(ControllerPaths.USER_NAME_HEADER) String username) {
        // The gateway URI-encodes the username so that any character fits in a header
        String decodedUsername;
        try {
            decodedUsername = URLDecoder.decode(username, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        // No control character (line break, NUL...) in a name other players will read
        decodedUsername = decodedUsername.replaceAll("\\p{Cc}", "");
        if (decodedUsername.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        EnsuredProfile result = profileService.ensureProfile(userId, decodedUsername);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ProfileResponse.of(result.profile()));
    }
}
