package com.openpoker.globalexception;

public class HostNotFoundException extends RuntimeException {
    public HostNotFoundException(String message) {
        super(message);
    }
}
