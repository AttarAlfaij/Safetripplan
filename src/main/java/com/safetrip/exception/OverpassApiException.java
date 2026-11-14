package com.safetrip.exception;

public class OverpassApiException extends RuntimeException {

    public OverpassApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public OverpassApiException(String message) {
        super(message);
    }
}

