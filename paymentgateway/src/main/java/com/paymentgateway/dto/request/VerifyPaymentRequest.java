package com.paymentgateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for verifying a PENDING payment.
 *
 * <p>Used after {@code initiatePayment()} for CARD and NET_BANKING modes.
 * Not required for UPI (resolved immediately during initiation).</p>
 */
@Data
@Schema(description = "Request payload to verify a pending payment")
public class VerifyPaymentRequest {

    @NotBlank(message = "Transaction ID is required")
    @Schema(description = "Transaction ID returned during payment initiation",
            example = "TXN-CARD-ABC123XYZ0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String transactionId;

    @Schema(description = "[CARD only] 6-digit OTP sent to the registered mobile number",
            example = "123456")
    private String otp;

    @Schema(description = "[NET_BANKING only] Confirmation token or bank callback token",
            example = "BANK-TOKEN-XYZ")
    private String bankConfirmationToken;
}
