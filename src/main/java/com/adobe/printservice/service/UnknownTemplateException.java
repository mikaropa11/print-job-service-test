package com.adobe.printservice.service;

public class UnknownTemplateException extends RuntimeException {

    public UnknownTemplateException(String templateId) {
        super("Unknown template: " + templateId);
    }
}
