package com.fileshare.repository;

import com.fileshare.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с метаданными файлов в PostgreSQL.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    /**
     * Возвращает все файлы указанного владельца.
     *
     * @param ownerId идентификатор владельца (sub из JWT)
     * @return список метаданных файлов
     */
    List<FileMetadata> findAllByOwnerIdOrderByUploadedAtDesc(String ownerId);

    /**
     * Находит файл по идентификатору и владельцу.
     *
     * @param id      идентификатор файла
     * @param ownerId идентификатор владельца
     * @return Optional с метаданными файла
     */
    Optional<FileMetadata> findByIdAndOwnerId(UUID id, String ownerId);
}
