package com.fileshare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конфигурационные свойства для ограничений загрузки файлов.
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /** Максимальный размер файла в байтах. */
    private long maxSizeBytes = 104857600L; // 100 MB

    /** Список запрещённых расширений файлов. */
    private List<String> forbiddenExtensions = List.of("exe", "bat", "sh", "cmd", "ps1");
}
