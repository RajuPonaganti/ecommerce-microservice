package com.ecommerce.order.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.dtos.OrderCreateReqDTO;
import com.ecommerce.order.dtos.OrderItemDetailDTO;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.events.EventName;
import com.ecommerce.order.events.OrderCreatedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
	private final OrderRepository orderRepository;
	private final KafkaTemplate<String, Object> KafkaTemplate;
	
	
	/*
	 * @CircuitBreaker(name = "inventory-service", fallbackMethod =
	 * "inventoryFallback") public OrderCreateRespDTO createOrder(OrderCreateReqDTO
	 * dto) { log.info("OrderService: createOrder() {}" + dto); // to do: how to
	 * check inventory is available for or not List<OrderItemDetailDTO> items =
	 * dto.getItems(); Order order =
	 * Order.builder().userId(dto.getUserId()).status(OrderStatus.CREATED).build();
	 * List<OrderItem> collect = items.stream().filter(Objects::nonNull) .map(item
	 * -> OrderItem.builder().productId(item.getProductId()).sku(item.getSku())
	 * .quantity(item.getQuantity()).unitPrice(item.getUnitPrice()).sellerId(item.
	 * getSellerId()) .torder(order).build()) .collect(Collectors.toList());
	 * order.setItems(collect); Order save = orderRepository.save(order);
	 * 
	 * OrderItemDetailDTO orderItemDetailDTO = dto.getItems().get(0); UUID productId
	 * = orderItemDetailDTO.getProductId(); int quantity =
	 * orderItemDetailDTO.getQuantity(); InventoryAvailabilityDTO availability =
	 * inventoryFeignClient.getAvailability(productId, quantity);
	 * System.out.println(availability); return null; }
	 */

	// Called automatically when circuit is OPEN or inventory-service returns errors
	// Signature must match the original method + add Throwable as last param
	/*
	 * public OrderCreateRespDTO inventoryFallback(OrderCreateReqDTO dto, Throwable
	 * ex) { throw new RuntimeException(
	 * "Inventory service is currently unavailable. Please try again shortly. Cause: "
	 * + ex.getMessage()); }
	 */

	@Transactional
	public UUID createOrder(OrderCreateReqDTO dto) {
		/*
		 * List<OrderItemDetailDTO> items = dto.getItems(); for (OrderItemDetailDTO
		 * orderItemDetailDTO : items) { UUID productId =
		 * orderItemDetailDTO.getProductId(); int quantity =
		 * orderItemDetailDTO.getQuantity(); InventoryAvailabilityDTO availability =
		 * inventoryFeignClient.getAvailability(productId, quantity);
		 * System.out.println(availability); }
		 */
		// to do check stock is present or not, after that lock
		// Let the DB generate the UUID — never set @Id manually on a @GeneratedValue entity
		Order order = Order.builder()
				.userId(dto.userId())
				.status(OrderStatus.CREATED)
				.finalAmount(dto.totalAmount())
				.discountAmount(dto.discount())
				.build();

		List<OrderItem> collect = dto.items().stream().map(e -> buildOrderItem(e, order)).collect(Collectors.toList());
		order.setItems(collect);
		Order save = orderRepository.save(order);
		OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), save.getOrderId(), Instant.now(), dto.items(), save.getFinalAmount(), save.getUserId());
		KafkaTemplate.send(EventName.ORDER_CREATED_EVENT_V1, save.getOrderId().toString(), event);
		return save.getOrderId();

	}

	private OrderItem buildOrderItem(OrderItemDetailDTO e, Order order) {
		return OrderItem.builder().productId(e.productId()).quantity(e.quantity()).unitPrice(e.unitPrice()).sku(e.sku())
				.torder(order).build();
	}

}
