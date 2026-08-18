package com.cryptotracker.watchlist;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptotracker.auth.LoginRequest;
import com.cryptotracker.auth.RegisterRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;

    @BeforeEach
    void authenticate() throws Exception {
        String email = "watchlist-test-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Str0ngPass!", "Watchlist Test"))));

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Str0ngPass!"))))
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addsAndListsAWatchlistItem() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coinId\":\"bitcoin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coinId").value("bitcoin"));

        mockMvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].coinId").value("bitcoin"));
    }

    @Test
    void addingTheSameCoinTwiceIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coinId\":\"ethereum\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/watchlist")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coinId\":\"ethereum\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void removesAWatchlistItem() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coinId\":\"solana\"}"));

        mockMvc.perform(delete("/api/watchlist/solana").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void removingAnItemNotInTheWatchlistReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/watchlist/doesnotexist").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void watchlistIsIsolatedPerUser() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coinId\":\"cardano\"}"));

        String otherEmail = "watchlist-other-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(otherEmail, "Str0ngPass!", "Other"))));
        String otherLoginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(otherEmail, "Str0ngPass!"))))
                .andReturn().getResponse().getContentAsString();
        String otherToken = objectMapper.readTree(otherLoginBody).get("accessToken").asString();

        mockMvc.perform(get("/api/watchlist").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
