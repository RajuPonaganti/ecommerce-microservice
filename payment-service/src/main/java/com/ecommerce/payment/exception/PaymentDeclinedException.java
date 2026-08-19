package com.ecommerce.payment.exception;

/** Card declined / invalid payment details — do not retry. */
public class PaymentDeclinedException extends PaymentGatewayException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
