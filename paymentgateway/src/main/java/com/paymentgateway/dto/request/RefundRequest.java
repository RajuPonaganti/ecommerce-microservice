package com.paymentgateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for initiating a full or partial refund.
 */
@Data
@Schema(description = "Request payload to initiate a refund")
public class RefundRequest {

    @NotBlank(message = "Transaction ID is required")
    @Schema(description = "Transaction ID of the original successful payment",
            example = "TXN-CARD-ABC123XYZ0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String transactionId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "1.00", message = "Refund amount must be at least 1.00")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(description = "Amount to refund. Must not exceed remaining refundable balance.",
            example = "500.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Reason for refund is required")
    @Schema(description = "Reason for the refund",
            example = "Product returned by customer",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
