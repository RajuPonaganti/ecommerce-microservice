package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Net Banking-specific details embedded in {@link PaymentResponse}.
 * Only populated when paymentMode = NET_BANKING.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Net Banking payment details (present only when paymentMode is NET_BANKING)")
public class NetBankingDetailsResponse {

    @Schema(description = "Bank code", example = "HDFC")
    private String bankCode;

    @Schema(description = "Full bank name", example = "HDFC Bank")
    private String bankName;

    @Schema(description = "Simulated bank portal authentication reference ID", example = "BANK-AUTH-AB12CD34")
    private String mockAuthRefId;
}
