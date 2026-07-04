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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты скачивания файлов.
 * Проверяет корректность содержимого и заголовков ответа.
 */
@AutoConfigureMockMvc
@DisplayName("FileDownload Integration Tests")
class FileDownloadIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private MinioStorageService storageService;

    private static final String USER_ID = "downloader-user-456";
    private static final String OTHER_USER_ID = "other-user-789";

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/files/{id}/download → скачивание своего файла")
    void download_ownFile_returnsFileContent() throws Exception {
        byte[] content = "Hello, World!".getBytes();
        String objectKey = USER_ID + "/test-download.txt";

        storageService.upload(objectKey, new ByteArrayInputStream(content), content.length, "text/plain");

        FileMetadata metadata = fileMetadataRepository.save(FileMetadata.builder()
                .ownerId(USER_ID)
                .originalFilename("test-download.txt")
                .objectKey(objectKey)
                .contentType("text/plain")
                .size((long) content.length)
                .uploadedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/files/" + metadata.getId() + "/download")
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "downloader"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("test-download.txt")))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("GET /api/files/{id}/download → скачивание чужого файла → 403")
    void download_otherUserFile_returns403() throws Exception {
        FileMetadata metadata = fileMetadataRepository.save(FileMetadata.builder()
                .ownerId(OTHER_USER_ID)
                .originalFilename("secret.txt")
                .objectKey(OTHER_USER_ID + "/secret.txt")
                .contentType("text/plain")
                .size(100L)
                .uploadedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/files/" + metadata.getId() + "/download")
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "downloader"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/files/{id}/download → несуществующий файл → 404")
    void download_notExistingFile_returns404() throws Exception {
        mockMvc.perform(get("/api/files/" + UUID.randomUUID() + "/download")
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "downloader"))))
                .andExpect(status().isNotFound());
    }
}
