package com.fileshare.exception;

/**
 * Исключение при нарушении ограничений на загружаемый файл.
 */
public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}
