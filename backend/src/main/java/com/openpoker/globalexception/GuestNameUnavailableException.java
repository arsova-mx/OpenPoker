package com.openpoker.globalexception;

public class GuestNameUnavailableException extends RuntimeException {
    public GuestNameUnavailableException(String message) {
        super(message);
    }
}
