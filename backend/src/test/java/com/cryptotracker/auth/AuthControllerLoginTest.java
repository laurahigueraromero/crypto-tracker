package com.cryptotracker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptotracker.users.User;
import com.cryptotracker.users.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userEmail;

    @BeforeEach
    void seedUser() {
        userEmail = "login-test-" + UUID.randomUUID() + "@example.com";
        User user = new User();
        user.setEmail(userEmail);
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!"));
        user.setDisplayName("Login Test");
        userRepository.save(user);
    }

    @Test
    void loginWithCorrectCredentialsReturnsTokens() throws Exception {
        LoginRequest request = new LoginRequest(userEmail, "Str0ngPass!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest(userEmail, "WrongPass1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithUnknownEmailReturnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("nobody@example.com", "Str0ngPass!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void accessingProtectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void accessingProtectedEndpointWithValidTokenIsAuthenticated() throws Exception {
        LoginRequest loginRequest = new LoginRequest(userEmail, "Str0ngPass!");
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).get("accessToken").asText();

        // No controller is mapped to /api/users/me yet, so a valid token still hits a 404
        // (proving the request passed authentication) rather than the 401 from the previous test.
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
