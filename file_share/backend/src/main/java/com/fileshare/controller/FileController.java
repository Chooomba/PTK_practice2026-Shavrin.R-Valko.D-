package com.fileshare.controller;

import com.fileshare.dto.FileResponseDto;
import com.fileshare.dto.ShareFilesRequestDto;
import com.fileshare.dto.UploadResponseDto;
import com.fileshare.service.FileService;
import com.fileshare.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for file operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, download, deletion, and sharing")
@SecurityRequirement(name = "oauth2")
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    /**
     * Returns files owned by the current user.
     *
     * @return file metadata DTOs
     */
    @GetMapping
    @Operation(summary = "Get current user's files")
    @ApiResponse(responseCode = "200", description = "File list")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<FileResponseDto>> listFiles() {
        userService.registerCurrentUser();
        return ResponseEntity.ok(fileService.listUserFiles());
    }

    /**
     * Returns metadata for a single file.
     *
     * @param id file identifier
     * @return file metadata DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get file information")
    @ApiResponse(responseCode = "200", description = "File information")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<FileResponseDto> getFileInfo(
            @Parameter(description = "File UUID") @PathVariable UUID id) {
        userService.registerCurrentUser();
        return ResponseEntity.ok(fileService.getFileInfo(id));
    }

    /**
     * Uploads a file for the current user.
     *
     * @param file uploaded file
     * @return upload result
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file")
    @ApiResponse(responseCode = "201", description = "File uploaded")
    @ApiResponse(responseCode = "400", description = "File validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<UploadResponseDto> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file) {
        userService.registerCurrentUser();
        UploadResponseDto response = fileService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Copies existing files from the current user to another known user.
     *
     * @param request recipient and file ids
     * @return copied files owned by the recipient
     */
    @PostMapping("/share")
    @Operation(summary = "Share existing files with another user")
    @ApiResponse(responseCode = "201", description = "Files shared")
    @ApiResponse(responseCode = "400", description = "Invalid share request")
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<List<FileResponseDto>> shareFiles(@Valid @RequestBody ShareFilesRequestDto request) {
        userService.registerCurrentUser();
        List<FileResponseDto> shared = fileService.shareExistingFiles(
                request.getRecipientUserId(),
                request.getFileIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(shared);
    }

    /**
     * Uploads files from the current device directly to another known user.
     *
     * @param recipientUserId target user id
     * @param files files to upload
     * @return uploaded files owned by the recipient
     */
    @PostMapping(value = "/share/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload files directly to another user")
    @ApiResponse(responseCode = "201", description = "Files uploaded to recipient")
    @ApiResponse(responseCode = "400", description = "File validation error")
    public ResponseEntity<List<FileResponseDto>> shareUploadedFiles(
            @RequestParam("recipientUserId") String recipientUserId,
            @RequestParam("files") List<MultipartFile> files) {
        userService.registerCurrentUser();
        List<FileResponseDto> uploaded = fileService.uploadFilesForRecipient(recipientUserId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    /**
     * Downloads a file by identifier.
     *
     * @param id file identifier
     * @return file stream
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "Download file")
    @ApiResponse(responseCode = "200", description = "File content")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "File UUID") @PathVariable UUID id) {
        userService.registerCurrentUser();
        FileService.FileDownloadResult result = fileService.downloadFile(id);
        String encodedFilename = URLEncoder.encode(result.metadata().getOriginalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_TYPE, result.metadata().getContentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.metadata().getSize()))
                .body(new InputStreamResource(result.stream()));
    }

    /**
     * Deletes a file by identifier.
     *
     * @param id file identifier
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete file")
    @ApiResponse(responseCode = "204", description = "File deleted")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "File UUID") @PathVariable UUID id) {
        userService.registerCurrentUser();
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}
