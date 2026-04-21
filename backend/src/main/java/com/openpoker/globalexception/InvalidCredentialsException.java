package com.openpoker.globalexception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciales invalidas");
    }
}
