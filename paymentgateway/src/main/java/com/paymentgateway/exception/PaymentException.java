package com.paymentgateway.exception;

import org.springframework.http.HttpStatus;

/**
 * General-purpose payment processing exception.
 * Carries an error code and HTTP status for structured error responses.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public PaymentException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
