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
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты удаления файлов.
 * Проверяет удаление из MinIO и из PostgreSQL.
 */
@AutoConfigureMockMvc
@DisplayName("FileDelete Integration Tests")
class FileDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private MinioStorageService storageService;

    private static final String USER_ID = "delete-user-111";
    private static final String OTHER_USER_ID = "other-delete-user-222";

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    @DisplayName("DELETE /api/files/{id} → удаляет файл из БД и MinIO")
    void delete_ownFile_removesFromDbAndStorage() throws Exception {
        byte[] content = "delete me".getBytes();
        String objectKey = USER_ID + "/to-delete.txt";
        storageService.upload(objectKey, new ByteArrayInputStream(content), content.length, "text/plain");

        FileMetadata metadata = fileMetadataRepository.save(FileMetadata.builder()
                .ownerId(USER_ID)
                .originalFilename("to-delete.txt")
                .objectKey(objectKey)
                .contentType("text/plain")
                .size((long) content.length)
                .uploadedAt(LocalDateTime.now())
                .build());

        UUID fileId = metadata.getId();

        mockMvc.perform(delete("/api/files/" + fileId)
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "deleter"))))
                .andExpect(status().isNoContent());

        assertFalse(fileMetadataRepository.existsById(fileId),
                "Запись о файле должна быть удалена из БД");
    }

    @Test
    @DisplayName("DELETE /api/files/{id} → удаление чужого файла → 403")
    void delete_otherUserFile_returns403() throws Exception {
        FileMetadata metadata = fileMetadataRepository.save(FileMetadata.builder()
                .ownerId(OTHER_USER_ID)
                .originalFilename("other.txt")
                .objectKey(OTHER_USER_ID + "/other.txt")
                .contentType("text/plain")
                .size(100L)
                .uploadedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/api/files/" + metadata.getId())
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "deleter"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/files/{id} → несуществующий файл → 404")
    void delete_notExistingFile_returns404() throws Exception {
        mockMvc.perform(delete("/api/files/" + UUID.randomUUID())
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "deleter"))))
                .andExpect(status().isNotFound());
    }
}
