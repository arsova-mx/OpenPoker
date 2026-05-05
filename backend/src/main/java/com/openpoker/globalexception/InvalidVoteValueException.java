package com.openpoker.globalexception;

public class InvalidVoteValueException extends RuntimeException {
    public InvalidVoteValueException(String message) {
        super(message);
    }
}
