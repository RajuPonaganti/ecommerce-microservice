package com.ecommerce.payment.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response from payment-gateway POST /api/v1/payments/initiate
 * and POST /api/v1/payments/verify
 */
@Getter
@Setter
@NoArgsConstructor
public class GatewayPaymentResponse {

    private String transactionId;
    private String status;          // "SUCCESS", "FAILED", "PENDING"
    private String message;
    private String orderId;
}
