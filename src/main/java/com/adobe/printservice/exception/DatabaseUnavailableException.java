package com.adobe.printservice.exception;

public class DatabaseUnavailableException extends RuntimeException {

    public DatabaseUnavailableException(Throwable cause) {
        super("Database is unavailable", cause);
    }
}
