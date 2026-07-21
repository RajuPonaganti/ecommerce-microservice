package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateDto {

	@Size(max = 500)
	private String title;

	private String description;

	@Size(max = 100)
	private String brand;

	@DecimalMin("0.01")
	private BigDecimal price;

	private BigDecimal mrp;

	private String currency;

	private String category;

	private Map<String, Object> attributes;
}
