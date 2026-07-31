package com.paymentgateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a transaction lookup by ID yields no result.
 */
public class TransactionNotFoundException extends PaymentException {

    public TransactionNotFoundException(String transactionId) {
        super(
            "Transaction not found with ID: " + transactionId,
            "TRANSACTION_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
}
