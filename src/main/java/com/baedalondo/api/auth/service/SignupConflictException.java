package com.baedalondo.api.auth.service;

public class SignupConflictException extends IllegalArgumentException {

    private final String field;

    public SignupConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
