package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Card-specific details embedded in {@link PaymentResponse}.
 * Only populated when paymentMode = CARD.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Card payment details (present only when paymentMode is CARD)")
public class CardDetailsResponse {

    @Schema(description = "Masked card number showing last 4 digits only", example = "**** **** **** 1111")
    private String cardNumberMasked;

    @Schema(description = "Bank Identification Number – first 6 digits", example = "411111")
    private String cardBin;

    @Schema(description = "Card network", example = "VISA",
            allowableValues = {"VISA", "Mastercard", "RuPay", "Amex", "Unknown"})
    private String cardNetwork;

    @Schema(description = "Card type", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
    private String cardType;

    @Schema(description = "Card expiry month (1–12)", example = "12")
    private int expiryMonth;

    @Schema(description = "Card expiry year (4-digit)", example = "2026")
    private int expiryYear;

    @Schema(description = "Card holder name as on card", example = "RAJESH KUMAR")
    private String cardHolderName;
}
