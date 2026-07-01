package com.fileshare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ShareFilesRequestDto {

    @NotBlank
    private String recipientUserId;

    @NotEmpty
    private List<UUID> fileIds;
}
