package com.ecommerce.order.dtos;

import java.math.BigDecimal;

/** Mirrors product-catalog-service's Product representation - what ProductClient deserializes into. */
public record ProductResponse(Long id, String name, BigDecimal price, Integer stockQuantity) {
}
