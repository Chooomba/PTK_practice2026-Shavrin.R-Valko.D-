package com.fileshare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO ответа на успешную загрузку файла.
 */
@Data
@Builder
@Schema(description = "Результат загрузки файла")
public class UploadResponseDto {

    @Schema(description = "Уникальный идентификатор загруженного файла")
    private UUID id;

    @Schema(description = "Имя файла")
    private String filename;

    @Schema(description = "Размер файла в байтах")
    private Long size;

    @Schema(description = "Сообщение о результате операции")
    private String message;
}
