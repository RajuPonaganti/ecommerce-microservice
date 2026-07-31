package com.paymentgateway.dto.request;

import com.paymentgateway.model.enums.BankCode;
import com.paymentgateway.model.enums.PaymentMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for initiating a new payment transaction.
 *
 * <p>Mode-specific fields:</p>
 * <ul>
 *   <li>CARD     – cardNumber, cardExpiry, cvv, cardHolderName</li>
 *   <li>NET_BANKING – bankCode</li>
 *   <li>UPI      – upiId</li>
 * </ul>
 */
@Data
@Schema(description = "Request payload to initiate a payment transaction")
public class InitiatePaymentRequest {

    @NotBlank(message = "Order ID is required")
    @Schema(description = "Unique order identifier from the merchant system", example = "ORD-20240101-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderId;

    @NotBlank(message = "Merchant ID is required")
    @Schema(description = "Registered merchant identifier", example = "MERCH-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(description = "Payment amount", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code")
    @Schema(description = "ISO 4217 currency code", example = "INR", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @NotNull(message = "Payment mode is required")
    @Schema(description = "Payment mode: CARD, NET_BANKING, or UPI", example = "CARD", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMode paymentMode;

    @NotBlank(message = "Customer name is required")
    @Schema(description = "Full name of the customer", example = "Rajesh Kumar", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @Email(message = "Valid email address is required")
    @NotBlank(message = "Customer email is required")
    @Schema(description = "Customer email address", example = "rajesh.kumar@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerEmail;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Valid 10-digit Indian mobile number required")
    @Schema(description = "Customer 10-digit mobile number", example = "9876543210")
    private String customerPhone;

    @Schema(description = "Brief description of the payment", example = "Purchase of Electronics")
    private String description;

    // ── CARD-specific ─────────────────────────────────────────────────────────

    @Schema(description = "[CARD only] 15–19 digit card number (spaces allowed)", example = "4111111111111111")
    private String cardNumber;

    @Schema(description = "[CARD only] Card expiry date in MM/YY format", example = "12/26")
    private String cardExpiry;

    @Schema(description = "[CARD only] 3 or 4 digit CVV/CVC", example = "123")
    private String cvv;

    @Schema(description = "[CARD only] Name of the card holder as printed on card", example = "RAJESH KUMAR")
    private String cardHolderName;

    // ── NET BANKING-specific ──────────────────────────────────────────────────

    @Schema(description = "[NET_BANKING only] Bank code, e.g. SBI, HDFC, ICICI, AXIS", example = "HDFC")
    private BankCode bankCode;

    // ── UPI-specific ──────────────────────────────────────────────────────────

    @Schema(description = "[UPI only] Virtual Payment Address / UPI ID (format: username@bankhandle)", example = "rajesh@okaxis")
    private String upiId;
}
