package com.fileshare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для отображения информации о файле пользователю.
 */
@Data
@Builder
@Schema(description = "Информация о файле")
public class FileResponseDto {

    @Schema(description = "Уникальный идентификатор файла")
    private UUID id;

    @Schema(description = "Имя файла")
    private String filename;

    @Schema(description = "Размер файла в байтах")
    private Long size;

    @Schema(description = "MIME-тип файла")
    private String contentType;

    @Schema(description = "Дата и время загрузки")
    private LocalDateTime uploadedAt;
}
