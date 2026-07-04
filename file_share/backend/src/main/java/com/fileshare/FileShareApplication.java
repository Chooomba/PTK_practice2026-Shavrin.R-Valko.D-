package com.fileshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа приложения FileShare.
 * Файловый обменник с авторизацией через Keycloak и хранением в MinIO.
 */
@SpringBootApplication
public class FileShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileShareApplication.class, args);
    }
}
