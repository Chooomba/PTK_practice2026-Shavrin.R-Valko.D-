package com.fileshare.service;

import com.fileshare.dto.UserResponseDto;
import com.fileshare.entity.AppUser;
import com.fileshare.exception.FileValidationException;
import com.fileshare.repository.AppUserRepository;
import com.fileshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;

    @Transactional
    public void registerCurrentUser() {
        String userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        String email = SecurityUtils.getCurrentUserEmail();

        AppUser user = appUserRepository.findById(userId)
                .orElseGet(() -> AppUser.builder()
                        .id(userId)
                        .createdAt(LocalDateTime.now())
                        .build());

        user.setUsername(username);
        user.setEmail(email);
        user.setLastSeenAt(LocalDateTime.now());
        appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> listRecipients() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        return appUserRepository.findAllByIdNotOrderByUsernameAsc(currentUserId)
                .stream()
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireKnownRecipient(String recipientUserId) {
        if (recipientUserId == null || recipientUserId.isBlank()) {
            throw new FileValidationException("Получатель не выбран");
        }
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(recipientUserId)) {
            throw new FileValidationException("Нельзя отправить файл самому себе");
        }
        if (!appUserRepository.existsById(recipientUserId)) {
            throw new FileValidationException("Получатель не найден");
        }
    }
}
