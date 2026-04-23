package com.openpoker.globalexception;

public class InvalidValue extends RuntimeException {
    public InvalidValue(String message) {
        super(message);
    }
}
