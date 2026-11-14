package com.safetrip.exception;

public class NoPlacesFoundException extends RuntimeException {

    public NoPlacesFoundException(String message) {
        super(message);
    }
}

