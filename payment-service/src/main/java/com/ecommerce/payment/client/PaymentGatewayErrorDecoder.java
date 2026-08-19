package com.ecommerce.payment.client;

import com.ecommerce.payment.exception.PaymentDeclinedException;
import com.ecommerce.payment.exception.PaymentGatewayAuthException;
import com.ecommerce.payment.exception.PaymentGatewayUnavailableException;
import com.ecommerce.payment.exception.PaymentGatewayException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates raw HTTP failures from the payment gateway into typed
 * exceptions the service layer can branch on, instead of leaking
 * generic FeignException everywhere.
 */
public class PaymentGatewayErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        // Never log response body here — may contain card data or secrets.
        log.warn("Payment gateway call [{}] failed with status {}", methodKey, status);

        switch (status) {
            case 400:
            case 422:
                // Card declined, invalid payment details, etc.
                return new PaymentDeclinedException(
                        "Payment gateway rejected the request: " + status);
            case 401:
            case 403:
                return new PaymentGatewayAuthException(
                        "Payment gateway authentication failed — check API key/secret");
            case 429:
                return new PaymentGatewayUnavailableException(
                        "Payment gateway rate-limited the request");
            case 500:
            case 502:
            case 503:
            case 504:
                return new PaymentGatewayUnavailableException(
                        "Payment gateway is currently unavailable: " + status);
            default:
                if (status >= 500) {
                    return new PaymentGatewayUnavailableException(
                            "Unexpected gateway server error: " + status);
                }
                return defaultDecoder.decode(methodKey, response);
        }
    }
}