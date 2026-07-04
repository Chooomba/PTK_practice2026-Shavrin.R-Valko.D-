package com.fileshare.controller;

import com.fileshare.dto.UserResponseDto;
import com.fileshare.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Known users available as file recipients")
@SecurityRequirement(name = "oauth2")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users available for file sharing")
    public ResponseEntity<List<UserResponseDto>> listUsers() {
        userService.registerCurrentUser();
        return ResponseEntity.ok(userService.listRecipients());
    }
}
