package com.cryptotracker.cryptos;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptotracker.auth.LoginRequest;
import com.cryptotracker.auth.RegisterRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CryptoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoinGeckoClient coinGeckoClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;

    @BeforeEach
    void authenticate() throws Exception {
        String email = "stats-test-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Str0ngPass!", "Stats Test"))));

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Str0ngPass!"))))
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asString();

        when(coinGeckoClient.fetchMarkets(anyInt(), anyInt(), anyString())).thenReturn(List.of(
                new CryptoMarketItem("bitcoin", "btc", "Bitcoin", "https://example.com/btc.png", 65000.0, 2.5, List.of(64000.0, 65000.0))
        ));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cryptos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsPaginatedMarketData() throws Exception {
        mockMvc.perform(get("/api/cryptos").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.perPage").value(50))
                .andExpect(jsonPath("$.items[0].coinId").value("bitcoin"))
                .andExpect(jsonPath("$.items[0].currentPrice").value(65000.0));
    }

    @Test
    void cachesRepeatedRequestsWithinTheSameWindow() throws Exception {
        mockMvc.perform(get("/api/cryptos?page=1&perPage=50&currency=usd")
                .header("Authorization", "Bearer " + accessToken));
        mockMvc.perform(get("/api/cryptos?page=1&perPage=50&currency=usd")
                .header("Authorization", "Bearer " + accessToken));

        verify(coinGeckoClient, times(1)).fetchMarkets(1, 50, "usd");
    }
}
