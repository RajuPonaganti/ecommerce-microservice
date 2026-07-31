package com.ecommerce.order.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.order.dtos.InventoryAvailabilityDTO;
import com.ecommerce.order.dtos.OrderCreateReqDTO;
import com.ecommerce.order.dtos.OrderCreateRespDTO;
import com.ecommerce.order.dtos.OrderItemDetailDTO;
import com.ecommerce.order.inventory.client.InventoryFeignClient;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrderService {
	private final InventoryFeignClient inventoryFeignClient;
	public OrderCreateRespDTO createOrder(OrderCreateReqDTO dto) {
		OrderItemDetailDTO orderItemDetailDTO = dto.getItems().get(0);
		UUID productId = orderItemDetailDTO.getProductId();
		int quantity = orderItemDetailDTO.getQuantity();
		InventoryAvailabilityDTO availability = inventoryFeignClient.getAvailability(productId, quantity);
		System.out.println(availability);
		return null;
	}

}
