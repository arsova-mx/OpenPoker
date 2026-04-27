package com.openpoker.globalexception;

public class InsufficientRoleException extends RuntimeException {
    public InsufficientRoleException(String message) {
        super(message);
    }
}
