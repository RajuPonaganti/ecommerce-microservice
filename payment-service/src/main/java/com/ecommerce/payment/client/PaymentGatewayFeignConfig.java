package com.ecommerce.payment.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Injects X-Api-Key and X-Api-Secret headers on every request
 * to the payment-gateway service — same as Razorpay's key/secret model.
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
            template.header("X-Api-Key",    apiKey);
            template.header("X-Api-Secret", apiSecret);
        };
    }
}
