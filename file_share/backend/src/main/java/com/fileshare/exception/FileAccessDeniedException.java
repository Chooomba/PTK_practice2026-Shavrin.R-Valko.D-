package com.fileshare.exception;

/**
 * Исключение при попытке доступа к чужому файлу.
 */
public class FileAccessDeniedException extends RuntimeException {
    public FileAccessDeniedException(String message) {
        super(message);
    }
}
