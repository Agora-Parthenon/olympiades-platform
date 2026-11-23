package org.olympiades.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.olympiades.platform.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.olympiades.platform.kafka.UserEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest extends TestcontainersConfiguration {

    @MockitoBean
    private UserEventPublisher events;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsTheProfileOnTheFirstCall() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/profiles/me").header("X-User-Id", id).header("X-User-Name", "api-newbie"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pseudo").value("api-newbie"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void returnsTheSameProfileOnTheNextCalls() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/profiles/me").header("X-User-Id", id).header("X-User-Name", "api-regular"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/profiles/me").header("X-User-Id", id).header("X-User-Name", "api-regular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pseudo").value("api-regular"));
    }

    @Test
    void refusesACallWithoutIdentity() throws Exception {
        mockMvc.perform(post("/profiles/me")).andExpect(status().isBadRequest());
    }

    @Test
    void refusesAUserIdThatIsNotAnUuid() throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", "not-a-uuid").header("X-User-Name", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesABlankUsername() throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", UUID.randomUUID()).header("X-User-Name", "  "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decodesAUsernameEncodedByTheGateway() throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", UUID.randomUUID()).header("X-User-Name", "Zo%C3%AB%20l%40mail"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pseudo").value("Zoë l@mail"));
    }

    @Test
    void refusesAMalformedlyEncodedUsername() throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", UUID.randomUUID()).header("X-User-Name", "%zz"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dropsControlCharactersFromTheUsername() throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", UUID.randomUUID()).header("X-User-Name", "ctrl%0A%00ok"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pseudo").value("ctrlok"));
    }

    @Test
    void refusesAUsernameMadeOnlyOfControlCharacters() throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", UUID.randomUUID()).header("X-User-Name", "%0A%09"))
                .andExpect(status().isBadRequest());
    }

    private void createProfile(UUID id, String pseudo) throws Exception {
        mockMvc.perform(post("/profiles/me").header("X-User-Id", id).header("X-User-Name", pseudo))
                .andExpect(status().isCreated());
    }

    @Test
    void showsMyProfileWithTheAvatarAndTheSignUpDate() throws Exception {
        UUID id = UUID.randomUUID();
        createProfile(id, "read-me");

        mockMvc.perform(get("/profiles/me").header("X-User-Id", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pseudo").value("read-me"))
                .andExpect(jsonPath("$.avatar").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void answers404WhenThePlayerHasNoProfileYet() throws Exception {
        mockMvc.perform(get("/profiles/me").header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("profile_not_found"));
    }

    @Test
    void updatesMyProfile() throws Exception {
        UUID id = UUID.randomUUID();
        createProfile(id, "to-update");

        mockMvc.perform(put("/profiles/me").header("X-User-Id", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pseudo\":\"updated-name\",\"avatar\":\"panda\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pseudo").value("updated-name"))
                .andExpect(jsonPath("$.avatar").value("panda"));

        mockMvc.perform(get("/profiles/me").header("X-User-Id", id))
                .andExpect(jsonPath("$.pseudo").value("updated-name"));
    }

    @Test
    void answers409WhenThePseudoIsAlreadyUsed() throws Exception {
        createProfile(UUID.randomUUID(), "already-here");
        UUID id = UUID.randomUUID();
        createProfile(id, "wants-it");

        mockMvc.perform(put("/profiles/me").header("X-User-Id", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pseudo\":\"Already-Here\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("pseudo_taken"));

        mockMvc.perform(get("/profiles/me").header("X-User-Id", id))
                .andExpect(jsonPath("$.pseudo").value("wants-it"));
    }

    @Test
    void answers400WhenThePseudoIsInvalid() throws Exception {
        UUID id = UUID.randomUUID();
        createProfile(id, "valid-name");

        mockMvc.perform(put("/profiles/me").header("X-User-Id", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pseudo\":\"no\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_pseudo"));
    }

    @Test
    void answers400WhenTheBodyIsNotJson() throws Exception {
        mockMvc.perform(put("/profiles/me").header("X-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsTheAvatarsToChooseFrom() throws Exception {
        mockMvc.perform(get("/profiles/avatars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("fox"))
                .andExpect(jsonPath("$.length()").value(8));
    }
}
