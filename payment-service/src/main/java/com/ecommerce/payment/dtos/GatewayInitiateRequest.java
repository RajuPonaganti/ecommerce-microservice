package com.ecommerce.payment.dtos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Request sent to payment-gateway POST /api/v1/payments/initiate
 */
@Getter
@Builder
public class GatewayInitiateRequest {

    private String orderId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String paymentMode;    // "UPI" — simplest for internal service-to-service
    private String upiId;          // internal service UPI ID for auto-debit simulation
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String description;
}
