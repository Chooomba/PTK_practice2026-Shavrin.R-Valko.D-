package com.fileshare.exception;

/**
 * Исключение при ошибках работы с S3/MinIO хранилищем.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
