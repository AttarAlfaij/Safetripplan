package com.safetrip.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPlanRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPlanRequest(InvalidPlanRequestException ex) {
        ApiErrorResponse body = new ApiErrorResponse("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCityNotFound(CityNotFoundException ex) {
        ApiErrorResponse body = new ApiErrorResponse("CITY_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoPlacesFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoPlacesFound(NoPlacesFoundException ex) {
        ApiErrorResponse body = new ApiErrorResponse("NO_PLACES_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(OverpassApiException.class)
    public ResponseEntity<ApiErrorResponse> handleOverpassError(OverpassApiException ex) {
        ApiErrorResponse body = new ApiErrorResponse("OVERPASS_API_ERROR", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        ApiErrorResponse body = new ApiErrorResponse("INTERNAL_ERROR", "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

