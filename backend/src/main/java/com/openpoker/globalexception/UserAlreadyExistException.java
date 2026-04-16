package com.openpoker.globalexception;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String field) {
        super(field + "ya existe");
    }
}
