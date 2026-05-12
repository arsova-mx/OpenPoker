package com.openpoker.globalexception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String field) {
        super("El usuario ya esta registrado con ese nombre: " + field);
    }
}
