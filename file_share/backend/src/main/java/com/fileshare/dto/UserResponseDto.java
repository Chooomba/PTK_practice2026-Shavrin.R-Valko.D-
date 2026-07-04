package com.fileshare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Known application user")
public class UserResponseDto {

    @Schema(description = "User identifier from JWT subject")
    private String id;

    @Schema(description = "Username from JWT preferred_username")
    private String username;

    @Schema(description = "User email")
    private String email;
}
