package com.ecommerce.payment.client;

import com.ecommerce.payment.dtos.GatewayApiResponse;
import com.ecommerce.payment.dtos.GatewayInitiateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for the internal payment-gateway service.
 *
 * The gateway enforces X-Api-Key + X-Api-Secret on /initiate and /verify.
 * These are injected via PaymentGatewayFeignConfig interceptor.
 */
@FeignClient(
        name = "payment-gateway",
        configuration = PaymentGatewayFeignConfig.class
)
public interface PaymentGatewayClient {

    /**
     * Initiate a payment.
     * Calls POST /api/v1/payments/initiate on payment-gateway.
     * UPI mode resolves immediately — returns SUCCESS or FAILED inline.
     */
    @PostMapping("/api/v1/payments/initiate")
    GatewayApiResponse initiatePayment(
            @RequestHeader("X-Api-Key")    String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @RequestBody GatewayInitiateRequest request);
}
