package com.fileshare.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Утилитный класс для получения информации о текущем пользователе из JWT.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Возвращает sub (subject) текущего аутентифицированного пользователя из JWT.
     *
     * @return sub пользователя
     * @throws IllegalStateException если пользователь не аутентифицирован
     */
    public static String getCurrentUserId() {
        return getJwt().getSubject();
    }

    /**
     * Возвращает preferred_username текущего пользователя из JWT.
     *
     * @return имя пользователя или sub, если preferred_username отсутствует
     */
    public static String getCurrentUsername() {
        Jwt jwt = getJwt();
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }

    /**
     * Возвращает email текущего пользователя из JWT.
     *
     * @return email или null, если отсутствует в токене
     */
    public static String getCurrentUserEmail() {
        return getJwt().getClaimAsString("email");
    }

    private static Jwt getJwt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new IllegalStateException("Пользователь не аутентифицирован");
    }
}
