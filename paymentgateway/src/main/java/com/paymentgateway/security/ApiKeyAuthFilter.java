package com.paymentgateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.model.entity.MerchantCredentials;
import com.paymentgateway.repository.MerchantCredentialsRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Razorpay-style API key + secret authentication filter.
 *
 * Every request to /api/v1/payments/initiate and /api/v1/payments/verify
 * must include:
 *
 *   X-Api-Key    : pgw_test_abc123xyz789   (public key — identifies the merchant)
 *   X-Api-Secret : rawSecretHere           (secret — verified against BCrypt hash)
 *
 * Optional per-merchant timeout headers (overridden by DB values):
 *   X-Connect-Timeout-Ms : 5000
 *   X-Read-Timeout-Ms    : 30000
 *
 * On success: MerchantCredentials is attached to request attribute "merchant"
 * On failure: 401 JSON response, request never reaches the controller
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY    = "X-Api-Key";
    public static final String HEADER_API_SECRET = "X-Api-Secret";
    public static final String ATTR_MERCHANT     = "merchant";

    private final MerchantCredentialsRepository credentialsRepository;
    private final PasswordEncoder               passwordEncoder;
    private final ObjectMapper                  objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String apiKey    = request.getHeader(HEADER_API_KEY);
        String apiSecret = request.getHeader(HEADER_API_SECRET);

        // 1. Validate headers present
        if (apiKey == null || apiKey.isBlank()) {
            reject(response, HttpStatus.UNAUTHORIZED,
                    "Missing required header: " + HEADER_API_KEY);
            return;
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            reject(response, HttpStatus.UNAUTHORIZED,
                    "Missing required header: " + HEADER_API_SECRET);
            return;
        }

        // 2. Look up merchant by api key
        Optional<MerchantCredentials> merchantOpt =
                credentialsRepository.findByApiKey(apiKey);

        if (merchantOpt.isEmpty()) {
            log.warn("Auth failed: unknown api_key={}", apiKey);
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid API credentials");
            return;
        }

        MerchantCredentials merchant = merchantOpt.get();

        // 3. Check merchant is active
        if (!merchant.isActive()) {
            log.warn("Auth failed: merchant {} is inactive", merchant.getMerchantId());
            reject(response, HttpStatus.FORBIDDEN,
                    "Merchant account is inactive. Contact support.");
            return;
        }

        // 4. Verify secret against BCrypt hash — timing-safe comparison
        if (!passwordEncoder.matches(apiSecret, merchant.getApiSecretHash())) {
            log.warn("Auth failed: wrong secret for merchant={}", merchant.getMerchantId());
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid API credentials");
            return;
        }

        // 5. Attach merchant to request for downstream use
        request.setAttribute(ATTR_MERCHANT, merchant);

        log.debug("Auth OK: merchant={} env={} connectTimeout={}ms readTimeout={}ms",
                merchant.getMerchantId(),
                merchant.getEnvironment(),
                merchant.getConnectTimeoutMs(),
                merchant.getReadTimeoutMs());

        chain.doFilter(request, response);
    }

    /** Only apply this filter to payment endpoints that require auth. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip auth for: swagger, api-docs, status check, list, validation-audit
        return path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/actuator")
                || (path.startsWith("/api/v1/payments") &&
                    !path.endsWith("/initiate") &&
                    !path.endsWith("/verify"))
                || path.startsWith("/api/v1/banks")
                || path.startsWith("/api/v1/refunds");
    }

    private void reject(HttpServletResponse response,
                        HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status",  status.value(),
                "error",   status.getReasonPhrase(),
                "message", message
        ));
    }
}
