package com.ecommerce.payment.client;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Retryer;
import feign.codec.ErrorDecoder;

/**
 * Feign configuration for the third-party payment-gateway service.
 * Injects auth headers, sets conservative timeouts, disables Feign's
 * built-in retrying (retries are handled explicitly at the call site
 * with idempotency keys — never auto-retry a payment POST blindly).
 */
@Configuration
public class PaymentGatewayFeignConfig {

    @Value("${payment.gateway.api-key}")
    private String apiKey;

    @Value("${payment.gateway.api-secret}")
    private String apiSecret;

    @Bean
    public RequestInterceptor paymentGatewayAuthInterceptor() {
        return (RequestTemplate template) -> {
            template.header("X-Api-Key", apiKey);
            template.header("X-Api-Secret", apiSecret);
        };
    }

    @Bean
    public Request.Options paymentGatewayRequestOptions() {
        // connectTimeout, readTimeout, followRedirects
        return new Request.Options(
                3, TimeUnit.SECONDS,
                10, TimeUnit.SECONDS,
                true
        );
    }

    @Bean
    public Retryer paymentGatewayRetryer() {
        // NEVER_RETRY at the Feign layer for payment-initiating calls —
        // a network-level retry on a POST that may have already reached
        // the gateway risks double-charging. Retries (if any) belong at
        // the business layer, gated by an idempotency key.
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public ErrorDecoder paymentGatewayErrorDecoder() {
        return new PaymentGatewayErrorDecoder();
    }
}