package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateDto {

	@NotBlank
	@Size(max = 500)
	private String title;

	private String description;

	@NotBlank
	@Size(max = 100)
	private String brand;

	@NotNull
	@DecimalMin("0.01")
	private BigDecimal price;

	private BigDecimal mrp;

	@NotBlank
	private String currency;

	@NotBlank
	private String category;

	@NotBlank
	@Size(max = 100)
	private String sku;

	private Map<String, Object> attributes;
}
