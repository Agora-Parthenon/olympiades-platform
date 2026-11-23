package org.olympiades.platform.controller;

import java.util.Map;

import org.olympiades.platform.exception.InvalidProfileException;
import org.olympiades.platform.exception.ProfileNotFoundException;
import org.olympiades.platform.exception.PseudoAlreadyUsedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns profile errors into a status and a stable {@code error} code the front can react to. */
@RestControllerAdvice
public class ProfileExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "profile_not_found"));
    }

    @ExceptionHandler(PseudoAlreadyUsedException.class)
    ResponseEntity<Map<String, String>> pseudoTaken() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "pseudo_taken"));
    }

    @ExceptionHandler(InvalidProfileException.class)
    ResponseEntity<Map<String, String>> invalid(InvalidProfileException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getCode(), "message", e.getMessage()));
    }
}
