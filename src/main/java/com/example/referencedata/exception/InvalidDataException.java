package com.example.referencedata.exception;

/** Данные запроса не соответствуют правилам предметной области (HTTP 400). */
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message) {
        super(message);
    }
}
