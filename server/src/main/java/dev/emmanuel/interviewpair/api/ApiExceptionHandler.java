package dev.emmanuel.interviewpair.api;

import dev.emmanuel.interviewpair.auth.AuthService.EmailTakenException;
import dev.emmanuel.interviewpair.auth.AuthService.InvalidCredentialsException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ErrorBody(String error, Map<String, String> fields) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorBody("Invalid request", fields));
    }

    @ExceptionHandler(EmailTakenException.class)
    ResponseEntity<ErrorBody> emailTaken(EmailTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorBody(e.getMessage(), Map.of()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorBody> badCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorBody(e.getMessage(), Map.of()));
    }
}
