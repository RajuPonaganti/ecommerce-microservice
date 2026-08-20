package com.ecommerce.user.dto;

import java.util.List;
import java.util.UUID;

public record UserCredentialsView(
        UUID userId,
        String email,
        String passwordHash,
        String status,
        boolean mfaEnabled,
        List<String> roles
) {}
