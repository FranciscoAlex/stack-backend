package com.stackcompany.stack.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Turns framework-level rejections into the {"message": "..."} shape the clients
 * already read. Without this, a bean-validation failure returns Spring's default
 * error body, which carries no usable "message" — so the UI could only show
 * "HTTP error! status: 400" and the actual reason was invisible to the user.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        if (details.isBlank()) details = "Pedido inválido.";
        log.warn("Validation failed: {}", details);

        return ResponseEntity.badRequest().body(new ErrorResponse(details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("Corpo do pedido inválido."));
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }
}
