package com.ecommerce.payment.service;

import org.springframework.stereotype.Component;

import com.ecommerce.payment.client.PaymentGatewayClient;
import com.ecommerce.payment.dtos.GatewayApiResponse;
import com.ecommerce.payment.dtos.GatewayInitiateRequest;
import com.ecommerce.payment.exception.PaymentGatewayUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Isolated component wrapping the payment-gateway Feign call with Resilience4j
 * CircuitBreaker + Retry. Kept as a separate bean (not a method inside
 * PaymentService) so Spring's AOP proxy can actually intercept the call —
 * self-invocation within the same class bypasses proxies and silently disables
 * the annotations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayInvoker {

	private final PaymentGatewayClient gatewayClient;

	@CircuitBreaker(name = "paymentGateway", fallbackMethod = "initiatePaymentFallback")
	@Retry(name = "paymentGateway")
	public GatewayApiResponse initiatePayment(String apiKey, String apiSecret, GatewayInitiateRequest request) {
		// orderId doubles as the idempotency key — stable per logical
		// payment attempt, so a Resilience4j retry after a timeout is
		// safe and won't double-charge.
		return gatewayClient.initiatePayment(apiKey, apiSecret, request);
	}

	/**
	 * Fallback signature must match the original method's params + a Throwable
	 * appended at the end. Called when the circuit is OPEN, or when all retry
	 * attempts are exhausted.
	 */
	private GatewayApiResponse initiatePaymentFallback(String apiKey, String apiSecret, GatewayInitiateRequest request,
			Throwable ex) {
		log.error("Payment gateway unavailable for orderId={} — circuit open or retries exhausted. Cause: {}", request.getOrderId(),
				ex.getMessage());
		throw new PaymentGatewayUnavailableException("Payment gateway unavailable for orderId=" + request.getOrderId());
	}
}
