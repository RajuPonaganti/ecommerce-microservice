package com.ecommerce.payment.exception;

/** Bad API key/secret — config issue, alert immediately, do not retry. */
public class PaymentGatewayAuthException extends PaymentGatewayException {
    public PaymentGatewayAuthException(String message) {
        super(message);
    }
}