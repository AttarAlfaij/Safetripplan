package com.safetrip.exception;

public class InvalidPlanRequestException extends RuntimeException {

    public InvalidPlanRequestException(String message) {
        super(message);
    }
}

