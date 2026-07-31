package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * UPI-specific details embedded in {@link PaymentResponse}.
 * Only populated when paymentMode = UPI.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "UPI payment details (present only when paymentMode is UPI)")
public class UpiDetailsResponse {

    @Schema(description = "Virtual Payment Address used", example = "rajesh@okaxis")
    private String vpa;

    @Schema(description = "Bank handle extracted from VPA (part after '@')", example = "okaxis")
    private String bankHandle;

    @Schema(description = "Internal UPI transaction reference ID (simulates NPCI ref)", example = "NPCI-AB12CD34EF56")
    private String upiTxnRefId;
}
