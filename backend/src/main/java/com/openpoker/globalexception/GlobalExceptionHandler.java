package com.openpoker.globalexception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<?> handleSessionNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyInSessionException.class)
    public ResponseEntity<?> handleUserAlreadyInSession(UserAlreadyInSessionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InsufficientRoleException.class)
    public ResponseEntity<?> handleInsufficientRole(InsufficientRoleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(HostNotFoundException.class)
    public ResponseEntity<?> handleHostNotFoundException(HostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(SessionNotInVotingException.class)
    public ResponseEntity<?> handleSessionNotInVotingException(SessionNotInVotingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(UsernameIsNotParticipantSessionException.class)
    public ResponseEntity<?> handleUsernameIsNotParticipantSessionException(UsernameIsNotParticipantSessionException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidValueException.class)
    public ResponseEntity<?> handleInvalidValue(InvalidValueException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(OnlyHostCanRevealVotesException.class)
    public ResponseEntity<?> handleOnlyHostCanRevealVotesException(OnlyHostCanRevealVotesException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidVoteValueException.class)
    public ResponseEntity<?> handleInvalidVoteValue(InvalidVoteValueException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DeckNotFoundException.class)
    public ResponseEntity<?> handleDeckNotFoundException(DeckNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        // "message" resume el primer error para que el frontend pueda mostrarlo directamente
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Datos inválidos");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message, "errors", errors));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequestsException(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of("message", ex.getMessage(), "retryAfterSeconds", ex.getRetryAfterSeconds()));
    }

    @ExceptionHandler(GuestNameUnavailableException.class)
    public ResponseEntity<?> handleGuestNameUnavailableException(GuestNameUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<?> handleTicketNotFoundException(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ParticipantNotFoundException.class)
    public ResponseEntity<?> handleParticipantNotFoundException(ParticipantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
