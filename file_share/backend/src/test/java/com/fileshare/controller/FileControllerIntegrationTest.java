package com.fileshare.controller;

import com.fileshare.entity.FileMetadata;
import com.fileshare.integration.AbstractIntegrationTest;
import com.fileshare.integration.MockJwtFactory;
import com.fileshare.repository.FileMetadataRepository;
import com.fileshare.storage.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для FileController.
 * Проверяет CRUD операции с файлами через MockMvc.
 */
@AutoConfigureMockMvc
@DisplayName("FileController Integration Tests")
class FileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    private static final String USER_ID = "user-abc-123";
    private static final String OTHER_USER_ID = "user-xyz-999";

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/files → список файлов текущего пользователя")
    void listFiles_authenticated_returnsOnlyOwnFiles() throws Exception {
        // Создаём файлы двух пользователей
        createTestMetadata(USER_ID, "my-file.txt");
        createTestMetadata(OTHER_USER_ID, "other-file.txt");

        mockMvc.perform(get("/api/files")
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "testuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("my-file.txt")));
    }

    @Test
    @DisplayName("GET /api/files/{id} → информация о своём файле")
    void getFileInfo_ownFile_returns200() throws Exception {
        FileMetadata metadata = createTestMetadata(USER_ID, "info-test.txt");

        mockMvc.perform(get("/api/files/" + metadata.getId())
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "testuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename", is("info-test.txt")))
                .andExpect(jsonPath("$.id", is(metadata.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/files/{id} → чужой файл возвращает 403")
    void getFileInfo_otherUserFile_returns403() throws Exception {
        FileMetadata metadata = createTestMetadata(OTHER_USER_ID, "secret.txt");

        mockMvc.perform(get("/api/files/" + metadata.getId())
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "testuser"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/files/{id} → несуществующий файл возвращает 404")
    void getFileInfo_notExistingFile_returns404() throws Exception {
        mockMvc.perform(get("/api/files/" + UUID.randomUUID())
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "testuser"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/files/{id} → удаление чужого файла возвращает 403")
    void deleteFile_otherUserFile_returns403() throws Exception {
        FileMetadata metadata = createTestMetadata(OTHER_USER_ID, "other.txt");

        mockMvc.perform(delete("/api/files/" + metadata.getId())
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "testuser"))))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private FileMetadata createTestMetadata(String ownerId, String filename) {
        return fileMetadataRepository.save(FileMetadata.builder()
                .ownerId(ownerId)
                .originalFilename(filename)
                .objectKey(ownerId + "/" + UUID.randomUUID() + "_" + filename)
                .contentType("text/plain")
                .size(1024L)
                .uploadedAt(LocalDateTime.now())
                .build());
    }
}
