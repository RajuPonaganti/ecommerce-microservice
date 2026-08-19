package com.ecommerce.payment.exception;


/** Gateway is down/overloaded — safe to retry later with backoff, or trip circuit breaker. */
public class PaymentGatewayUnavailableException extends PaymentGatewayException {
    public PaymentGatewayUnavailableException(String message) {
        super(message);
    }
}
