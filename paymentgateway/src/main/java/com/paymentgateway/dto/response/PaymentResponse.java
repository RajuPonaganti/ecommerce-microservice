package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unified payment response returned for all payment modes.
 *
 * <p>Mode-specific details are in dedicated nested objects
 * ({@code cardDetails}, {@code upiDetails}, {@code netBankingDetails}).
 * Exactly one of these is populated based on {@code paymentMode};
 * the others are omitted ({@code @JsonInclude(NON_NULL)}).</p>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified payment response — mode-specific details are in nested objects")
public class PaymentResponse {

    // ── Core transaction fields ───────────────────────────────────────────────

    @Schema(description = "Unique gateway-assigned transaction identifier", example = "TXN-CARD-ABC123XYZ0")
    private String transactionId;

    @Schema(description = "Merchant order ID", example = "ORD-20240101-001")
    private String orderId;

    @Schema(description = "Merchant ID", example = "MERCH-001")
    private String merchantId;

    @Schema(description = "Payment amount", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", example = "INR")
    private String currency;

    @Schema(description = "Payment mode used", example = "CARD")
    private PaymentMode paymentMode;

    @Schema(description = "Current transaction status",
            example = "SUCCESS",
            allowableValues = {"INITIATED", "PENDING", "SUCCESS", "FAILED", "REFUNDED", "PARTIALLY_REFUNDED"})
    private PaymentStatus status;

    @Schema(description = "Gateway internal reference ID for reconciliation", example = "GW-REF-789XYZ")
    private String gatewayReferenceId;

    @Schema(description = "Failure reason (only when status = FAILED)",
            example = "Insufficient balance in linked account.")
    private String failureReason;

    @Schema(description = "Customer name", example = "Rajesh Kumar")
    private String customerName;

    @Schema(description = "Customer email", example = "rajesh.kumar@email.com")
    private String customerEmail;

    @Schema(description = "Customer phone", example = "9876543210")
    private String customerPhone;

    // ── Mode-specific detail objects ──────────────────────────────────────────

    @Schema(description = "Card payment details (present only when paymentMode = CARD)")
    private CardDetailsResponse cardDetails;

    @Schema(description = "UPI payment details (present only when paymentMode = UPI)")
    private UpiDetailsResponse upiDetails;

    @Schema(description = "Net Banking details (present only when paymentMode = NET_BANKING)")
    private NetBankingDetailsResponse netBankingDetails;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Schema(description = "Transaction creation timestamp (UTC)")
    private LocalDateTime createdAt;

    @Schema(description = "Transaction completion timestamp (UTC, present only on SUCCESS/REFUNDED)")
    private LocalDateTime completedAt;

    @Schema(description = "Context-sensitive message for the current state",
            example = "OTP has been sent to your registered mobile number.")
    private String message;
}
