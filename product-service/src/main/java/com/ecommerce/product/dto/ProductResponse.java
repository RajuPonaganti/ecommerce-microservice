package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirrors product-catalog-service's Product representation - what ProductClient
 * deserializes into.
 */
public record ProductResponse(UUID productId, String name, BigDecimal price, Integer stockQuantity) {

	public ProductResponse(UUID productId, String name, BigDecimal price, Integer stockQuantity) {
		this.name = name;
		this.price = price;
		this.productId = productId;
		this.stockQuantity = stockQuantity;
	}
}
