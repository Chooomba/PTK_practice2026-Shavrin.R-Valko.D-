package com.fileshare.controller;

import com.fileshare.integration.AbstractIntegrationTest;
import com.fileshare.integration.MockJwtFactory;
import com.fileshare.repository.AppUserRepository;
import com.fileshare.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("File sharing integration tests")
class FileSharingIntegrationTest extends AbstractIntegrationTest {

    private static final String SENDER_ID = "sender-user-123";
    private static final String RECEIVER_ID = "receiver-user-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/users returns known users except current user")
    void listUsers_returnsKnownRecipients() throws Exception {
        registerUser(SENDER_ID, "sender");
        registerUser(RECEIVER_ID, "receiver");

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(MockJwtFactory.create(SENDER_ID, "sender"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(RECEIVER_ID)))
                .andExpect(jsonPath("$[0].username", is("receiver")));
    }

    @Test
    @DisplayName("POST /api/files/share copies existing uploaded files to recipient")
    void shareExistingFiles_copiesFilesToRecipient() throws Exception {
        registerUser(RECEIVER_ID, "receiver");
        String uploadedFileId = uploadFileAsSender("shared-note.txt", "text/plain", "hello receiver");

        mockMvc.perform(post("/api/files/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientUserId": "receiver-user-456",
                                  "fileIds": ["%s"]
                                }
                                """.formatted(uploadedFileId))
                        .with(jwt().jwt(MockJwtFactory.create(SENDER_ID, "sender"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("shared-note.txt")))
                .andExpect(jsonPath("$[0].id", notNullValue()));

        mockMvc.perform(get("/api/files")
                        .with(jwt().jwt(MockJwtFactory.create(RECEIVER_ID, "receiver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("shared-note.txt")));
    }

    @Test
    @DisplayName("POST /api/files/share/upload uploads device files directly to recipient")
    void shareUploadedFiles_uploadsFilesToRecipient() throws Exception {
        registerUser(RECEIVER_ID, "receiver");

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "device-report.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/files/share/upload")
                        .file(file)
                        .param("recipientUserId", RECEIVER_ID)
                        .with(jwt().jwt(MockJwtFactory.create(SENDER_ID, "sender"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("device-report.pdf")));

        mockMvc.perform(get("/api/files")
                        .with(jwt().jwt(MockJwtFactory.create(RECEIVER_ID, "receiver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename", is("device-report.pdf")));
    }

    @Test
    @DisplayName("POST /api/files/share/upload reports validation reason")
    void shareUploadedFiles_forbiddenExtension_reportsReason() throws Exception {
        registerUser(RECEIVER_ID, "receiver");

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "script.sh",
                "text/x-shellscript",
                "echo bad".getBytes()
        );

        mockMvc.perform(multipart("/api/files/share/upload")
                        .file(file)
                        .param("recipientUserId", RECEIVER_ID)
                        .with(jwt().jwt(MockJwtFactory.create(SENDER_ID, "sender"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("расширением .sh")))
                .andExpect(jsonPath("$.detail", containsString("запрещена")));
    }

    private void registerUser(String userId, String username) throws Exception {
        mockMvc.perform(get("/api/files")
                        .with(jwt().jwt(MockJwtFactory.create(userId, username))))
                .andExpect(status().isOk());
    }

    private String uploadFileAsSender(String filename, String contentType, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content.getBytes());

        return mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(jwt().jwt(MockJwtFactory.create(SENDER_ID, "sender"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
