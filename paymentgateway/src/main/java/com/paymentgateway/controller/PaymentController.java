package com.paymentgateway.controller;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.dto.response.ApiResponse;
import com.paymentgateway.dto.response.PaymentResponse;
import com.paymentgateway.dto.response.ValidationAuditResponse;
import com.paymentgateway.model.entity.ValidationAudit;
import com.paymentgateway.service.PaymentService;
import com.paymentgateway.service.validation.ValidationAuditService;
import com.paymentgateway.model.entity.MerchantCredentials;
import com.paymentgateway.security.ApiKeyAuthFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing payment lifecycle APIs.
 *
 * <pre>
 * POST  /api/v1/payments/initiate                        – start a new payment
 * POST  /api/v1/payments/verify                          – verify OTP / bank callback
 * GET   /api/v1/payments/{txnId}/status                  – query payment status
 * GET   /api/v1/payments/{txnId}/validation-audit        – view all validation rule results
 * GET   /api/v1/payments                                 – list all transactions
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "APIs for initiating, verifying, and querying payment transactions")
public class PaymentController {

    private final PaymentService         paymentService;
    private final ValidationAuditService auditService;

    // ── POST /initiate ────────────────────────────────────────────────────────

    @Operation(
            summary     = "Initiate a payment",
            description = """
                    Creates a new payment transaction for the specified payment mode.
                    
                    **CARD** – All fields validated (Luhn algorithm, expiry, CVV format).
                    CVV is discarded after validation and **never stored**.
                    Sets status to `PENDING`; call `/verify` with the OTP to complete.
                    
                    **NET_BANKING** – Bank code validated against the active bank master table.
                    Sets status to `PENDING`; call `/verify` after mock bank authentication.
                    
                    **UPI** – VPA format validated and bank handle checked against master data.
                    Resolves immediately — returns `SUCCESS` or `FAILED` inline.
                    
                    Every validation rule run is recorded in the `validation_audit` table.
                    Retrieve them via `GET /{transactionId}/validation-audit`.
                    
                    Duplicate `orderId` values are rejected with HTTP 409.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Payment initiated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error or missing mode-specific fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Duplicate order ID")
    })
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            HttpServletRequest httpRequest) {

        MerchantCredentials merchant =
                (MerchantCredentials) httpRequest.getAttribute(ApiKeyAuthFilter.ATTR_MERCHANT);

        log.info("POST /initiate | orderId={} | mode={} | merchant={} | connectTimeout={}ms | readTimeout={}ms",
                request.getOrderId(),
                request.getPaymentMode(),
                merchant != null ? merchant.getMerchantId() : "unknown",
                merchant != null ? merchant.getConnectTimeoutMs() : "-",
                merchant != null ? merchant.getReadTimeoutMs() : "-");

        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, response.getMessage()));
    }

    // ── POST /verify ──────────────────────────────────────────────────────────

    @Operation(
            summary     = "Verify a pending payment",
            description = """
                    Completes a `PENDING` payment:
                    
                    - **CARD**: Submit the 6-digit OTP. Simulation rates: 75% success,
                      10% wrong-OTP, 15% insufficient balance.
                    - **NET_BANKING**: Submit any non-blank `bankConfirmationToken`.
                      Simulation rates: 85% success, 5% bank timeout, 10% invalid credentials.
                    - **UPI**: No-op — UPI resolves immediately during initiation.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Verification processed (check `status` for SUCCESS/FAILED)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Transaction not in PENDING state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Transaction not found")
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            HttpServletRequest httpRequest) {

        MerchantCredentials merchant =
                (MerchantCredentials) httpRequest.getAttribute(ApiKeyAuthFilter.ATTR_MERCHANT);

        log.info("POST /verify | txnId={} | merchant={}",
                request.getTransactionId(),
                merchant != null ? merchant.getMerchantId() : "unknown");

        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    // ── GET /{transactionId}/status ───────────────────────────────────────────

    @Operation(
            summary     = "Get payment status",
            description = "Returns the current status and full details (including mode-specific nested object) of a transaction."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Transaction not found")
    })
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @Parameter(description = "Gateway-issued transaction ID", example = "TXN-CARD-ABC123XYZ0")
            @PathVariable String transactionId) {
        log.info("GET /{}/status", transactionId);
        PaymentResponse response = paymentService.paymentStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction status retrieved successfully."));
    }

    // ── GET /{transactionId}/validation-audit ────────────────────────────────

    @Operation(
            summary     = "Get validation audit trail",
            description = """
                    Returns every validation rule that was executed for this transaction,
                    in chronological order. Each record shows the rule name, PASS/FAIL result,
                    and a descriptive message.
                    
                    Useful for debugging rejected payments and verifying that specific checks
                    (e.g. Luhn, expiry, bank active) were actually run.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Audit trail retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Transaction not found")
    })
    @GetMapping("/{transactionId}/validation-audit")
    public ResponseEntity<ApiResponse<List<ValidationAuditResponse>>> getValidationAudit(
            @Parameter(description = "Gateway-issued transaction ID", example = "TXN-CARD-ABC123XYZ0")
            @PathVariable String transactionId) {
        log.info("GET /{}/validation-audit", transactionId);

        // Ensure the transaction exists (throws 404 if not)
        paymentService.paymentStatus(transactionId);

        List<ValidationAuditResponse> audit = auditService.getAuditTrail(transactionId)
                .stream()
                .map(this::toAuditResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(audit,
                "Retrieved " + audit.size() + " validation audit record(s) for transaction " + transactionId + "."));
    }

    // ── GET / (list all) ──────────────────────────────────────────────────────

    @Operation(
            summary     = "List all transactions",
            description = "Returns every payment transaction in the system. Intended for admin and testing use."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Transactions retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllTransactions() {
        log.info("GET /payments – listing all transactions");
        List<PaymentResponse> list = paymentService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.success(list,
                "Retrieved " + list.size() + " transaction(s)."));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ValidationAuditResponse toAuditResponse(ValidationAudit a) {
        return ValidationAuditResponse.builder()
                .transactionId(a.getTransactionId())
                .ruleName(a.getRuleName())
                .result(a.getResult())
                .message(a.getMessage())
                .paymentMode(a.getPaymentMode())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
