package com.fileshare.service;

import com.fileshare.config.FileProperties;
import com.fileshare.dto.FileResponseDto;
import com.fileshare.dto.UploadResponseDto;
import com.fileshare.entity.FileMetadata;
import com.fileshare.exception.FileAccessDeniedException;
import com.fileshare.exception.FileNotFoundException;
import com.fileshare.exception.FileValidationException;
import com.fileshare.mapper.FileMetadataMapper;
import com.fileshare.repository.FileMetadataRepository;
import com.fileshare.security.SecurityUtils;
import com.fileshare.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for upload, listing, download, deletion, and user-to-user file sharing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final MinioStorageService storageService;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileProperties fileProperties;
    private final UserService userService;

    /**
     * Returns files owned by the current user.
     *
     * @return file metadata DTOs
     */
    @Transactional(readOnly = true)
    public List<FileResponseDto> listUserFiles() {
        String userId = SecurityUtils.getCurrentUserId();
        log.debug("Listing files for user: {}", userId);
        List<FileMetadata> files = fileMetadataRepository.findAllByOwnerIdOrderByUploadedAtDesc(userId);
        return fileMetadataMapper.toResponseDtoList(files);
    }

    /**
     * Returns metadata for a single file owned by the current user.
     *
     * @param fileId file identifier
     * @return file metadata DTO
     */
    @Transactional(readOnly = true)
    public FileResponseDto getFileInfo(UUID fileId) {
        FileMetadata metadata = getOwnedFile(fileId);
        return fileMetadataMapper.toResponseDto(metadata);
    }

    /**
     * Uploads a file for the current user.
     *
     * @param file uploaded multipart file
     * @return upload result
     */
    @Transactional
    public UploadResponseDto uploadFile(MultipartFile file) {
        FileMetadata saved = uploadForOwner(file, SecurityUtils.getCurrentUserId());
        return UploadResponseDto.builder()
                .id(saved.getId())
                .filename(saved.getOriginalFilename())
                .size(saved.getSize())
                .message("Файл успешно загружен")
                .build();
    }

    /**
     * Copies existing files owned by the current user to another known user.
     *
     * @param recipientUserId target user id
     * @param fileIds source file ids
     * @return copied file metadata DTOs for the recipient
     */
    @Transactional
    public List<FileResponseDto> shareExistingFiles(String recipientUserId, List<UUID> fileIds) {
        userService.requireKnownRecipient(recipientUserId);
        if (fileIds == null || fileIds.isEmpty()) {
            throw new FileValidationException("Выберите хотя бы один файл");
        }

        String senderId = SecurityUtils.getCurrentUserId();
        List<FileMetadata> sharedFiles = new ArrayList<>();
        for (UUID fileId : fileIds) {
            FileMetadata source = getOwnedFile(fileId);
            String targetObjectKey = buildObjectKey(recipientUserId, source.getOriginalFilename());
            storageService.copy(source.getObjectKey(), targetObjectKey);

            FileMetadata copy = FileMetadata.builder()
                    .ownerId(recipientUserId)
                    .originalFilename(source.getOriginalFilename())
                    .objectKey(targetObjectKey)
                    .contentType(source.getContentType())
                    .size(source.getSize())
                    .build();
            sharedFiles.add(fileMetadataRepository.save(copy));
            log.info("File shared: sourceId={}, sender={}, recipient={}", fileId, senderId, recipientUserId);
        }

        return fileMetadataMapper.toResponseDtoList(sharedFiles);
    }

    /**
     * Uploads files from the sender's device directly to the recipient account.
     *
     * @param recipientUserId target user id
     * @param files uploaded multipart files
     * @return uploaded file metadata DTOs for the recipient
     */
    @Transactional
    public List<FileResponseDto> uploadFilesForRecipient(String recipientUserId, List<MultipartFile> files) {
        userService.requireKnownRecipient(recipientUserId);
        if (files == null || files.isEmpty()) {
            throw new FileValidationException("Выберите хотя бы один файл");
        }

        List<FileMetadata> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            savedFiles.add(uploadForOwner(file, recipientUserId));
        }
        return fileMetadataMapper.toResponseDtoList(savedFiles);
    }

    /**
     * Downloads a file owned by the current user.
     *
     * @param fileId file identifier
     * @return metadata and content stream
     */
    @Transactional(readOnly = true)
    public FileDownloadResult downloadFile(UUID fileId) {
        FileMetadata metadata = getOwnedFile(fileId);
        InputStream stream = storageService.download(metadata.getObjectKey());
        log.info("File downloaded: id={}, user={}", fileId, SecurityUtils.getCurrentUserId());
        return new FileDownloadResult(metadata, stream);
    }

    /**
     * Deletes a file owned by the current user.
     *
     * @param fileId file identifier
     */
    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata metadata = getOwnedFile(fileId);
        storageService.delete(metadata.getObjectKey());
        fileMetadataRepository.delete(metadata);
        log.info("File deleted: id={}, user={}", fileId, SecurityUtils.getCurrentUserId());
    }

    private FileMetadata uploadForOwner(MultipartFile file, String ownerId) {
        validateFile(file);

        String filename = requireOriginalFilename(file);
        String objectKey = buildObjectKey(ownerId, filename);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            storageService.upload(objectKey, inputStream, file.getSize(), contentType);
        } catch (IOException e) {
            log.error("Failed to read uploaded file stream", e);
            throw new FileValidationException("Не удалось прочитать файл");
        }

        FileMetadata metadata = FileMetadata.builder()
                .ownerId(ownerId)
                .originalFilename(filename)
                .objectKey(objectKey)
                .contentType(contentType)
                .size(file.getSize())
                .build();

        FileMetadata saved = fileMetadataRepository.save(metadata);
        log.info("File uploaded: id={}, name={}, owner={}", saved.getId(), saved.getOriginalFilename(), ownerId);
        return saved;
    }

    private FileMetadata getOwnedFile(UUID fileId) {
        String userId = SecurityUtils.getCurrentUserId();
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + fileId));

        if (!metadata.getOwnerId().equals(userId)) {
            log.warn("Access denied: user={} tried to access file={} owned by {}",
                    userId, fileId, metadata.getOwnerId());
            throw new FileAccessDeniedException("Доступ к файлу запрещен");
        }

        return metadata;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Файл не может быть пустым");
        }

        String filename = requireOriginalFilename(file);
        if (file.getSize() > fileProperties.getMaxSizeBytes()) {
            throw new FileValidationException(
                    "Файл превышает максимально допустимый размер: " +
                            fileProperties.getMaxSizeBytes() / 1024 / 1024 + " МБ");
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = filename.substring(dotIndex + 1).toLowerCase();
            if (fileProperties.getForbiddenExtensions().contains(extension)) {
                throw new FileValidationException("Загрузка файлов с расширением ." + extension + " запрещена");
            }
        }
    }

    private String requireOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileValidationException("Имя файла не указано");
        }

        String filename = Paths.get(originalFilename).getFileName().toString();
        if (filename.isBlank() || ".".equals(filename) || "..".equals(filename)) {
            throw new FileValidationException("Некорректное имя файла");
        }
        return filename;
    }

    private String buildObjectKey(String ownerId, String filename) {
        return ownerId + "/" + UUID.randomUUID() + "_" + filename;
    }

    /**
     * Download result containing metadata and object stream.
     */
    public record FileDownloadResult(FileMetadata metadata, InputStream stream) {}
}
