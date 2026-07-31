package com.ecommerce.order.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Setter
@Getter
@NoArgsConstructor
public class OrderCreateReqDTO {
	private UUID userId;    // Who placed the order (reference to User Service)

    private List<OrderItemDetailDTO> items;


    private BigDecimal totalAmount;
}
