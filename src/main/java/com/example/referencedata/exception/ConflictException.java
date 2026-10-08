package com.example.referencedata.exception;

/** Операция конфликтует с текущим состоянием данных (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
