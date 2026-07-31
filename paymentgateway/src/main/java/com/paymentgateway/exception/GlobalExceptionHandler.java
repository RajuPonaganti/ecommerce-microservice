package com.paymentgateway.exception;

import com.paymentgateway.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handling – converts all exceptions into
 * the standard {@link ApiResponse} error format.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Domain exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFound(TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return buildError(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(PaymentException ex) {
        log.warn("Payment exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildError(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus());
    }

    // ── Validation exceptions ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return buildError("Request validation failed.", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, details);
    }

    // ── Catch-all ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return buildError(
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Void>> buildError(String message, String errorCode, HttpStatus status) {
        return buildError(message, errorCode, status, null);
    }

    private ResponseEntity<ApiResponse<Void>> buildError(
            String message, String errorCode, HttpStatus status, String errorDetail) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .errorCode(errorCode)
                .error(errorDetail)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
