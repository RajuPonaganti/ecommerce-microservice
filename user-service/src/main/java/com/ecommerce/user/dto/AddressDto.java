package com.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddressDto(

		UUID addressId,

		@NotBlank String label,

		@NotBlank String street,

		@NotBlank String city,

		@NotNull Integer pincode,

		boolean isDefault) {
}
