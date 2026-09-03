package com.adobe.printservice.exception;

public class UnknownTemplateException extends RuntimeException {

    public UnknownTemplateException(String templateId) {
        super("Unknown template: " + templateId);
    }
}
