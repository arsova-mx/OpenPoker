package com.openpoker.globalexception;

public class OnlyHostCanRevealVotesException extends RuntimeException {
    public OnlyHostCanRevealVotesException(String message) {
        super(message);
    }
}
