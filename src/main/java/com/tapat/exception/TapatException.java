package com.tapat.exception;

public class TapatException extends RuntimeException {
    public TapatException(String message) {
        super(message);
    }

    public TapatException(String message, Throwable cause) {
        super(message, cause);
    }
}
