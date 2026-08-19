package com.ecommerce.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ecommerce.payment.dtos.GatewayApiResponse;
import com.ecommerce.payment.dtos.GatewayInitiateRequest;

/**
 * Feign client for the third-party payment-gateway service.
 * Auth headers (X-Api-Key / X-Api-Secret) are injected automatically
 * by PaymentGatewayFeignConfig — callers don't pass them.
 */
@FeignClient(
        name = "payment-gateway",
        url = "${payment.gateway.base-url}",
        configuration = PaymentGatewayFeignConfig.class
)
public interface PaymentGatewayClient {

    /**
     * Initiate a payment. Calls POST /api/v1/payments/initiate on payment-gateway.
     * UPI mode resolves immediately — returns SUCCESS or FAILED inline.
     *
     * idempotencyKey MUST be stable per logical payment attempt (e.g. your
     * internal payment/order id) so a client-side retry after a timeout
     * doesn't double-charge.
     */
    @PostMapping("/api/v1/payments/initiate")
    GatewayApiResponse initiatePayment(@RequestHeader("X-Api-Key") String apiKey,
			@RequestHeader("X-Api-Secret") String apiSecret,
            @RequestBody GatewayInitiateRequest request);
}
