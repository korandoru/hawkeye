package io.korandoru.hawkeye.core;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
