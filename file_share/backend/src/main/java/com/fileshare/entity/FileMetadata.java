package com.fileshare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность метаданных файла.
 * Хранит информацию о загруженном файле: владелец, имя, ключ объекта в S3, тип и размер.
 */
@Entity
@Table(name = "file_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    /** Уникальный идентификатор записи. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Идентификатор владельца файла (sub из JWT). */
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    /** Оригинальное имя файла при загрузке. */
    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    /** Ключ объекта в MinIO (S3). */
    @Column(name = "object_key", nullable = false, unique = true, length = 1000)
    private String objectKey;

    /** MIME-тип файла. */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** Размер файла в байтах. */
    @Column(name = "size", nullable = false)
    private Long size;

    /** Время загрузки файла. */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
