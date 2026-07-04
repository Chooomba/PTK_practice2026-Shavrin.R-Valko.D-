package com.fileshare.mapper;

import com.fileshare.dto.FileResponseDto;
import com.fileshare.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct маппер для преобразования FileMetadata в DTO.
 */
@Mapper(componentModel = "spring")
public interface FileMetadataMapper {

    /**
     * Преобразует сущность FileMetadata в FileResponseDto.
     *
     * @param entity сущность метаданных файла
     * @return DTO с информацией о файле
     */
    @Mapping(source = "originalFilename", target = "filename")
    FileResponseDto toResponseDto(FileMetadata entity);

    /**
     * Преобразует список сущностей в список DTO.
     *
     * @param entities список сущностей метаданных
     * @return список DTO
     */
    List<FileResponseDto> toResponseDtoList(List<FileMetadata> entities);
}
