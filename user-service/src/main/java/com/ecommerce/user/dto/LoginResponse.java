package com.ecommerce.user.dto;

import java.util.UUID;

public record LoginResponse(

        String accessToken,
        String tokenType,       // always "Bearer"
        long expiresInMs,
        UUID userId,
        String email
) {}
