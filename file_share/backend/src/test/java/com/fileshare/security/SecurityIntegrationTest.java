package com.fileshare.security;

import com.fileshare.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты безопасности.
 * Проверяет, что запросы без JWT возвращают 401 Unauthorized.
 */
@AutoConfigureMockMvc
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/files без токена → 401")
    void listFiles_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/files/upload без токена → 401")
    void uploadFile_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/files/upload"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/files/{id}/download без токена → 401")
    void downloadFile_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/files/00000000-0000-0000-0000-000000000001/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/files/{id} без токена → 401")
    void deleteFile_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/files/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Swagger UI доступен без токена")
    void swaggerUi_noToken_returns200() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()); // Swagger редиректит на /swagger-ui/index.html
    }
}
