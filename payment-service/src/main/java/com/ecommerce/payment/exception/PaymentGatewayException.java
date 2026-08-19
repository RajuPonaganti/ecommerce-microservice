package com.ecommerce.payment.exception;

/** Base type for all payment-gateway related failures. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }
}
