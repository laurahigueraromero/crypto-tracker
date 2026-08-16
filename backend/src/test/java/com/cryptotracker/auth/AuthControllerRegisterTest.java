package com.cryptotracker.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptotracker.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerRegisterTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Test
    void registersNewUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("laura@example.com", "Str0ngPass!", "Laura");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("laura@example.com"))
                .andExpect(jsonPath("$.displayName").value("Laura"));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsByEmail("laura@example.com"));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("dup@example.com", "Str0ngPass!", "Laura");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void rejectsWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("weak@example.com", "weak", "Laura");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0]").value(containsString("password")));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findByEmail("weak@example.com").isEmpty());
    }
}
