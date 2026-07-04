package com.fileshare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI/Swagger.
 * Описывает API и схему авторизации через Keycloak OAuth2.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Создаёт конфигурацию OpenAPI с поддержкой OAuth2 авторизации.
     *
     * @return объект OpenAPI
     */
    @Bean
    public OpenAPI openAPI() {
        String authUrl = issuerUri + "/protocol/openid-connect";

        return new OpenAPI()
                .info(new Info()
                        .title("FileShare API")
                        .description("API для загрузки, скачивания и управления файлами")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authUrl + "/auth")
                                                .tokenUrl(authUrl + "/token")
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect scope")
                                                        .addString("profile", "Profile scope")
                                                        .addString("email", "Email scope")
                                                )
                                        )
                                )
                        )
                );
    }
}
