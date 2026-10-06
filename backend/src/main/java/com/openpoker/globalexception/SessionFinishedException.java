package com.openpoker.globalexception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SessionFinishedException extends RuntimeException {
    public SessionFinishedException(String message) {
        super(message);
    }
}