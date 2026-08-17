package com.ecommerce.order.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreateReqDTO(UUID userId, List<OrderItemDetailDTO> items, BigDecimal totalAmount, BigDecimal discount) {
	
}
