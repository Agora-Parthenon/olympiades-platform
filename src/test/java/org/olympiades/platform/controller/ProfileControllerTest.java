package org.olympiades.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.olympiades.platform.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest extends TestcontainersConfiguration {

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
}
