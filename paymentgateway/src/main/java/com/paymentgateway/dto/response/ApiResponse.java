package com.paymentgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Unified API response wrapper returned for every endpoint.
 *
 * @param <T> payload type
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiResponse<T> {

    @Schema(description = "Whether the request succeeded", example = "true")
    private boolean success;

    @Schema(description = "HTTP status code mirrored in body", example = "200")
    private int statusCode;

    @Schema(description = "Human-readable response message", example = "Payment initiated successfully.")
    private String message;

    @Schema(description = "Response payload (null on error)")
    private T data;

    @Schema(description = "Error code (present only on failure)", example = "TRANSACTION_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Detailed error description (present only on failure)")
    private String error;

    @Builder.Default
    @Schema(description = "UTC timestamp of the response")
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Factory helpers ───────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(201)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, String error, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .error(error)
                .build();
    }
}
