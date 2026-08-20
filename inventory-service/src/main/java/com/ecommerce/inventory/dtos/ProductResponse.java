package com.ecommerce.inventory.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors product-catalog-service's Product representation - what ProductClient
 * deserializes into.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
	private Long id;
	private String name;
	private BigDecimal price;
	private Integer stockQuantity;
}
