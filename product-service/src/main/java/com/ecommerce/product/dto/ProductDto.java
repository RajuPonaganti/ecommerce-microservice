package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ecommerce.product.model.ProductStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {

	private UUID productId;
	private String title;
	private String description;
	private String brand;
	private BigDecimal price;
	private BigDecimal mrp;
	private String currency;
	private String category;
	private ProductStatus status;
	private Map<String, Object> attributes;
	private UUID sellerId;
	private String sku;
	private Instant createdAt;
	private Instant updatedAt;
	private Long version;
}
