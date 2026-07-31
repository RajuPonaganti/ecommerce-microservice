package com.ecommerce.order.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ecommerce.order.clients.InventoryFeignClient;
import com.ecommerce.order.dtos.InventoryAvailabilityDTO;
import com.ecommerce.order.dtos.OrderCreateReqDTO;
import com.ecommerce.order.dtos.OrderCreateRespDTO;
import com.ecommerce.order.dtos.OrderItemDetailDTO;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
	private final InventoryFeignClient inventoryFeignClient;
	private final OrderRepository orderRepository;

	public OrderCreateRespDTO createOrder(OrderCreateReqDTO dto) {
		log.info("OrderService: createOrder() {}" + dto);
		// to do: how to check inventory is available for or not
		List<OrderItemDetailDTO> items = dto.getItems();
		Order order = Order.builder().userId(dto.getUserId()).status(OrderStatus.CREATED).build();
		List<OrderItem> collect = items.stream().filter(Objects::nonNull)
				.map(item -> OrderItem.builder().productId(item.getProductId()).sku(item.getSku())
						.quantity(item.getQuantity()).unitPrice(item.getUnitPrice()).sellerId(item.getSellerId())
						.torder(order).build())
				.collect(Collectors.toList());
		order.setItems(collect);
		Order save = orderRepository.save(order);
		
		OrderItemDetailDTO orderItemDetailDTO = dto.getItems().get(0);
		UUID productId = orderItemDetailDTO.getProductId();
		int quantity = orderItemDetailDTO.getQuantity();
		InventoryAvailabilityDTO availability = inventoryFeignClient.getAvailability(productId, quantity);
		System.out.println(availability);
		return null;
	}

}
