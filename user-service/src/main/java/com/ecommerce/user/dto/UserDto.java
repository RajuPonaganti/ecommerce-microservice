package com.ecommerce.user.dto;

import com.ecommerce.user.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID userId,
        String email,
        String phoneNumber,
        UserStatus status,
        boolean mfaEnabled,
        Instant createdAt,
        Instant updatedAt
) {}
