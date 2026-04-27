package com.openpoker.globalexception;

public class UserAlreadyInSessionException extends RuntimeException {
    public UserAlreadyInSessionException(String message) {
        super(message);
    }
}
