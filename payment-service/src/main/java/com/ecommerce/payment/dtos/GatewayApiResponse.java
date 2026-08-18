package com.ecommerce.payment.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Wrapper matching payment-gateway's ApiResponse<PaymentResponse> envelope.
 */
@Getter
@Setter
@NoArgsConstructor
public class GatewayApiResponse {

    private int status;
    private String message;
    private GatewayPaymentResponse data;
}
