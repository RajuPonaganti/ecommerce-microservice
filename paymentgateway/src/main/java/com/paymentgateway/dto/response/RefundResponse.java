package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response returned after creating or querying a refund.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Refund transaction response")
public class RefundResponse {

    @Schema(description = "Unique refund identifier", example = "RFD-ABC1234567")
    private String refundId;

    @Schema(description = "Original transaction ID", example = "TXN-CARD-ABC123XYZ0")
    private String transactionId;

    @Schema(description = "Amount refunded in this operation", example = "500.00")
    private BigDecimal refundAmount;

    @Schema(description = "Original transaction amount", example = "1500.00")
    private BigDecimal originalAmount;

    @Schema(description = "Refund processing status", example = "SUCCESS")
    private String status;

    @Schema(description = "Reason provided for the refund", example = "Product returned by customer")
    private String reason;

    @Schema(description = "Timestamp when the refund was initiated")
    private LocalDateTime initiatedAt;

    @Schema(description = "Timestamp when the refund was processed")
    private LocalDateTime processedAt;

    @Schema(description = "Estimated time for amount to reflect in customer's account",
            example = "5-7 business days")
    private String estimatedCreditDays;
}
