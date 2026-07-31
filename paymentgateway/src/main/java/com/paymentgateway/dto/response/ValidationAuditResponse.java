package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a single validation rule audit entry returned from
 * {@code GET /api/v1/payments/{transactionId}/validation-audit}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Validation audit record for a single rule check")
public class ValidationAuditResponse {

    @Schema(description = "Transaction ID this rule was checked against", example = "TXN-CARD-ABC123XYZ0")
    private String transactionId;

    @Schema(description = "Rule name", example = "CARD_LUHN_CHECK")
    private String ruleName;

    @Schema(description = "Rule result", example = "PASS", allowableValues = {"PASS", "FAIL"})
    private String result;

    @Schema(description = "Descriptive message for the rule outcome",
            example = "Card number passed Luhn algorithm check.")
    private String message;

    @Schema(description = "Payment mode the rule applies to", example = "CARD")
    private String paymentMode;

    @Schema(description = "Timestamp when the rule was evaluated")
    private LocalDateTime createdAt;
}
