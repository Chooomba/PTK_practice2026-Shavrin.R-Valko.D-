package com.fileshare.exception;

/**
 * Исключение при отсутствии файла в системе.
 */
public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) {
        super(message);
    }
}
