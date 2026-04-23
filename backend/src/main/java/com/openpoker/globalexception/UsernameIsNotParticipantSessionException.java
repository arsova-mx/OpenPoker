package com.openpoker.globalexception;

public class UsernameIsNotParticipantSessionException extends RuntimeException {
    public UsernameIsNotParticipantSessionException(String message) {
        super(message);
    }
}
