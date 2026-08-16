package com.cryptotracker.notes;

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
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;

    @BeforeEach
    void authenticate() throws Exception {
        String email = "notes-test-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Str0ngPass!", "Notes Test"))));

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Str0ngPass!"))))
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    void createsNoteAssociatedToMultipleCoins() throws Exception {
        String payload = """
                {
                  "title": "BTC vs ETH",
                  "content": "Comparativa de fundamentales",
                  "type": "OBSERVACION",
                  "coinIds": ["bitcoin", "ethereum"],
                  "tags": ["comparativa", "largo-plazo"]
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("BTC vs ETH"))
                .andExpect(jsonPath("$.type").value("OBSERVACION"))
                .andExpect(jsonPath("$.coinIds.length()").value(2))
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void rejectsNoteWithoutCoinIds() throws Exception {
        String payload = """
                {
                  "title": "Sin cripto",
                  "content": "No debería guardarse",
                  "type": "OBSERVACION",
                  "coinIds": []
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsInvalidNoteType() throws Exception {
        String payload = """
                {
                  "title": "Tipo invalido",
                  "content": "No debería guardarse",
                  "type": "OPINION",
                  "coinIds": ["bitcoin"]
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void sanitizesHtmlInContent() throws Exception {
        String payload = """
                {
                  "title": "Contenido con script",
                  "content": "<script>alert(1)</script> hola",
                  "type": "PREDICCION",
                  "coinIds": ["bitcoin"]
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("&lt;script&gt;alert(1)&lt;/script&gt; hola"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        String payload = """
                {
                  "title": "Sin sesion",
                  "content": "No debería guardarse",
                  "type": "OBSERVACION",
                  "coinIds": ["bitcoin"]
                }
                """;

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
