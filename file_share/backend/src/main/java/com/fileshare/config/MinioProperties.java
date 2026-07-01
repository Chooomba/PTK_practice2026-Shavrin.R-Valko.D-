package com.fileshare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства для подключения к MinIO.
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** URL эндпоинта MinIO. */
    private String endpoint;

    /** Ключ доступа (access key). */
    private String accessKey;

    /** Секретный ключ. */
    private String secretKey;

    /** Имя bucket для хранения файлов. */
    private String bucket;
}
