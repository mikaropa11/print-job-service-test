package com.adobe.printservice.exception;

public class TransientRenderingException extends RuntimeException {

    public TransientRenderingException(String message) {
        super(message);
    }

    public TransientRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
