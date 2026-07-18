package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 7, max = 15)
        String phone,

        @NotBlank @Size(min = 8, max = 100)
        String password
) {}
