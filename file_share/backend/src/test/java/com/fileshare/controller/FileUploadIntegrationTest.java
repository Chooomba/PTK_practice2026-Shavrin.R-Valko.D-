package com.fileshare.controller;

import com.fileshare.integration.AbstractIntegrationTest;
import com.fileshare.integration.MockJwtFactory;
import com.fileshare.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты загрузки файлов.
 * Проверяет сохранение в MinIO и запись метаданных в PostgreSQL.
 */
@AutoConfigureMockMvc
@DisplayName("FileUpload Integration Tests")
class FileUploadIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    private static final String USER_ID = "uploader-user-123";

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/files/upload → файл загружается, метаданные сохраняются")
    void upload_validFile_savesMetadataAndReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "uploader"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename", is("test-document.pdf")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.message", notNullValue()));

        assertEquals(1, fileMetadataRepository.findAllByOwnerIdOrderByUploadedAtDesc(USER_ID).size());
    }

    @Test
    @DisplayName("POST /api/files/upload → запрещённое расширение возвращает 400")
    void upload_forbiddenExtension_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "some bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "uploader"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("расширением .exe")))
                .andExpect(jsonPath("$.detail", containsString("запрещена")));
    }

    @Test
    @DisplayName("POST /api/files/upload → пустой файл возвращает 400")
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "uploader"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/files/upload → изображение загружается корректно")
    void upload_imageFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                new byte[]{(byte)0x89, 0x50, 0x4E, 0x47} // PNG magic bytes
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(jwt().jwt(MockJwtFactory.create(USER_ID, "uploader"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename", is("photo.png")));
    }
}
