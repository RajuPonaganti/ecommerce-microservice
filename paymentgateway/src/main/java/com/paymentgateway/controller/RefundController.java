package com.paymentgateway.controller;

import com.paymentgateway.dto.request.RefundRequest;
import com.paymentgateway.dto.response.ApiResponse;
import com.paymentgateway.dto.response.RefundResponse;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.entity.Refund;
import com.paymentgateway.repository.RefundRepository;
import com.paymentgateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for refund operations.
 *
 * <pre>
 * POST  /api/v1/refunds          – initiate a full or partial refund
 * GET   /api/v1/refunds/{id}     – retrieve refund details
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Refunds", description = "APIs for initiating and querying refunds")
public class RefundController {

    private final PaymentService    paymentService;
    private final RefundRepository  refundRepository;

    // ── POST / ────────────────────────────────────────────────────────────────

    @Operation(
            summary     = "Initiate a refund",
            description = """
                    Initiates a full or partial refund for a `SUCCESS` or `PARTIALLY_REFUNDED` transaction.
                    
                    **Rules:**
                    - Only `SUCCESS` or `PARTIALLY_REFUNDED` transactions are eligible.
                    - The total refunded amount across all refund requests for one transaction cannot exceed
                      the original payment amount.
                    - Partial refunds are allowed; the transaction status changes to `PARTIALLY_REFUNDED`.
                    - A full refund changes the transaction status to `REFUNDED`.
                    
                    **Credit timeline:** 5–7 business days (simulated).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Refund initiated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Transaction not eligible or refund amount exceeds limit"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Transaction not found")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> initiateRefund(
            @Valid @RequestBody RefundRequest request) {

        log.info("POST /refunds | transactionId={} | amount={}", request.getTransactionId(), request.getAmount());
        RefundResponse response = paymentService.refund(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response,
                        "Refund initiated successfully. Amount will be credited in "
                                + response.getEstimatedCreditDays() + "."));
    }

    // ── GET /{refundId} ───────────────────────────────────────────────────────

    @Operation(
            summary     = "Get refund details",
            description = "Retrieves the details of a specific refund by its unique refund ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Refund found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Refund not found")
    })
    @GetMapping("/{refundId}")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(
            @Parameter(description = "Unique refund identifier", example = "RFD-ABC1234567")
            @PathVariable String refundId) {

        log.info("GET /refunds/{}", refundId);

        Refund refund = refundRepository.findByRefundId(refundId)
                .orElseThrow(() -> new PaymentException(
                        "Refund not found with ID: " + refundId,
                        "REFUND_NOT_FOUND",
                        HttpStatus.NOT_FOUND));

        RefundResponse response = RefundResponse.builder()
                .refundId(refund.getRefundId())
                .transactionId(refund.getTransaction().getTransactionId())
                .refundAmount(refund.getAmount())
                .originalAmount(refund.getTransaction().getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .initiatedAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .estimatedCreditDays("5-7 business days")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Refund details retrieved successfully."));
    }
}
