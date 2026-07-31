package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO representing a bank from the master data table.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Bank master data record")
public class BankResponse {

    @Schema(description = "Short bank code used in API requests", example = "HDFC")
    private String bankCode;

    @Schema(description = "Full bank name", example = "HDFC Bank")
    private String bankName;

    @Schema(description = "IFSC prefix", example = "HDFC")
    private String ifscPrefix;

    @Schema(description = "Comma-separated UPI handles associated with this bank",
            example = "okhdfc,hdfcbank")
    private String upiHandles;

    @Schema(description = "Whether this bank supports Net Banking payments", example = "true")
    private boolean supportsNetBanking;

    @Schema(description = "Whether this bank supports UPI payments", example = "true")
    private boolean supportsUpi;

    @Schema(description = "Whether this bank is currently active", example = "true")
    private boolean isActive;
}
