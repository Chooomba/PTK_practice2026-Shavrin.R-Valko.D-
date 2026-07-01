package com.fileshare.storage;

import com.fileshare.config.MinioProperties;
import com.fileshare.exception.StorageException;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.CopySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Service for file operations in MinIO, an S3-compatible object storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * Uploads a file object to MinIO.
     *
     * @param objectKey key in object storage
     * @param inputStream file content stream
     * @param size file size in bytes
     * @param contentType MIME type
     */
    public void upload(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            log.debug("Uploading object to MinIO: key={}, size={}, contentType={}", objectKey, size, contentType);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("File uploaded to MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", objectKey, e);
            throw new StorageException("Не удалось загрузить файл в хранилище", e);
        }
    }

    /**
     * Copies a file object inside the configured bucket.
     *
     * @param sourceObjectKey source object key
     * @param targetObjectKey target object key
     */
    public void copy(String sourceObjectKey, String targetObjectKey) {
        try {
            log.debug("Copying object in MinIO: source={}, target={}", sourceObjectKey, targetObjectKey);
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(targetObjectKey)
                            .source(CopySource.builder()
                                    .bucket(minioProperties.getBucket())
                                    .object(sourceObjectKey)
                                    .build())
                            .build()
            );
            log.info("File copied in MinIO: {} -> {}", sourceObjectKey, targetObjectKey);
        } catch (Exception e) {
            log.error("Failed to copy file in MinIO: {} -> {}", sourceObjectKey, targetObjectKey, e);
            throw new StorageException("Не удалось скопировать файл в хранилище", e);
        }
    }

    /**
     * Downloads a file object from MinIO.
     *
     * @param objectKey key in object storage
     * @return file content stream
     */
    public InputStream download(String objectKey) {
        try {
            log.debug("Downloading object from MinIO: {}", objectKey);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", objectKey, e);
            throw new StorageException("Не удалось скачать файл из хранилища", e);
        }
    }

    /**
     * Deletes a file object from MinIO.
     *
     * @param objectKey key in object storage
     */
    public void delete(String objectKey) {
        try {
            log.debug("Deleting object from MinIO: {}", objectKey);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
            log.info("File deleted from MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectKey, e);
            throw new StorageException("Не удалось удалить файл из хранилища", e);
        }
    }
}
