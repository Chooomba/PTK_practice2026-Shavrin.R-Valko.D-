package com.fileshare.integration;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Фабрика для создания тестовых JWT-токенов.
 */
public final class MockJwtFactory {

    private MockJwtFactory() {}

    /**
     * Создаёт тестовый JWT для указанного пользователя.
     *
     * @param userId   идентификатор пользователя (sub)
     * @param username имя пользователя
     * @return mock JWT
     */
    public static Jwt create(String userId, String username) {
        return Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", username)
                .claim("email", username + "@test.com")
                .claim("realm_access", Map.of("roles", java.util.List.of("user")))
                .build();
    }

    /**
     * Создаёт тестовый JWT с автогенерацией UUID пользователя.
     *
     * @param username имя пользователя
     * @return mock JWT
     */
    public static Jwt create(String username) {
        return create(UUID.randomUUID().toString(), username);
    }
}
