package com.paymentgateway.controller;

import com.paymentgateway.dto.response.ApiResponse;
import com.paymentgateway.dto.response.BankResponse;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.entity.Bank;
import com.paymentgateway.repository.BankRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for bank master data.
 *
 * <pre>
 * GET  /api/v1/banks                    – list all active banks
 * GET  /api/v1/banks/netbanking         – list banks supporting net banking
 * GET  /api/v1/banks/upi                – list banks supporting UPI
 * GET  /api/v1/banks/{bankCode}         – get a specific bank by code
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banks", description = "Bank master data – query supported banks and their capabilities")
public class BankController {

    private final BankRepository bankRepository;

    @Operation(summary = "List all active banks",
               description = "Returns all banks currently active in the system.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Banks retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<BankResponse>>> getAllActiveBanks() {
        List<BankResponse> banks = bankRepository.findByIsActiveTrueOrderByBankName()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(banks, "Retrieved " + banks.size() + " active bank(s)."));
    }

    @Operation(summary = "List banks supporting Net Banking",
               description = "Returns active banks that support Net Banking payments.")
    @GetMapping("/netbanking")
    public ResponseEntity<ApiResponse<List<BankResponse>>> getNetBankingBanks() {
        List<BankResponse> banks = bankRepository
                .findBySupportsNetBankingTrueAndIsActiveTrueOrderByBankName()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(banks,
                "Retrieved " + banks.size() + " bank(s) supporting Net Banking."));
    }

    @Operation(summary = "List banks supporting UPI",
               description = "Returns active banks that support UPI payments.")
    @GetMapping("/upi")
    public ResponseEntity<ApiResponse<List<BankResponse>>> getUpiBanks() {
        List<BankResponse> banks = bankRepository
                .findBySupportsUpiTrueAndIsActiveTrueOrderByBankName()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(banks,
                "Retrieved " + banks.size() + " bank(s) supporting UPI."));
    }

    @Operation(summary = "Get bank by code",
               description = "Returns details for a single bank identified by its bank code.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Bank found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Bank not found")
    })
    @GetMapping("/{bankCode}")
    public ResponseEntity<ApiResponse<BankResponse>> getBankByCode(
            @Parameter(description = "Bank code, e.g. HDFC, SBI", example = "HDFC")
            @PathVariable String bankCode) {
        Bank bank = bankRepository.findByBankCodeIgnoreCase(bankCode)
                .orElseThrow(() -> new PaymentException(
                        "Bank not found with code: " + bankCode,
                        "BANK_NOT_FOUND", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success(toResponse(bank), "Bank retrieved successfully."));
    }

    private BankResponse toResponse(Bank b) {
        return BankResponse.builder()
                .bankCode(b.getBankCode())
                .bankName(b.getBankName())
                .ifscPrefix(b.getIfscPrefix())
                .upiHandles(b.getUpiHandles())
                .supportsNetBanking(b.isSupportsNetBanking())
                .supportsUpi(b.isSupportsUpi())
                .isActive(b.isActive())
                .build();
    }
}
