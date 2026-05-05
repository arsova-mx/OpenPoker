package com.openpoker.globalexception;

public class SessionNotInVotingException extends RuntimeException {
    public SessionNotInVotingException(String message) {
        super(message);
    }
}
