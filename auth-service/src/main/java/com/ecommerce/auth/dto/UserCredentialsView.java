package com.ecommerce.auth.dto;

import java.util.UUID;

/**
 * Mirrors the response shape of user-service's internal
 * {@code GET /internal/users/credentials?email=} endpoint.
 * Never exposed externally — the API gateway must block /internal/**.
 */
public record UserCredentialsView(
        UUID userId,
        String email,
        String passwordHash,
        String status,      // e.g. ACTIVE, PENDING_VERIFICATION, DELETED
        boolean mfaEnabled,
        java.util.List<String> roles
) {}
