package com.ecommerce.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 100)
        String displayName,

        @Size(min = 7, max = 15)
        String phone
) {}
