package com.openpoker.globalexception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String field) {
        super("El campo '" + field + "' ya existe");
    }
}
