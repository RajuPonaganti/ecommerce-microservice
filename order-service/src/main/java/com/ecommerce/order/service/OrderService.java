package com.ecommerce.order.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.commons.EventName;
import com.ecommerce.commons.InventoryReservationFailedEvent;
import com.ecommerce.commons.OrderCreatedEvent;
import com.ecommerce.commons.OrderItemDetailDTO;
import com.ecommerce.commons.PaymentFailedEvent;
import com.ecommerce.order.dtos.OrderCreateReqDTO;
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

	private final OrderRepository orderRepository;
	private final KafkaTemplate<String, Object> KafkaTemplate;

	// ── Create Order ──────────────────────────────────────────────────────────

	@Transactional
	public UUID createOrder(OrderCreateReqDTO dto) {
		Order order = Order.builder()
				.userId(dto.userId())
				.status(OrderStatus.CREATED)
				.finalAmount(dto.totalAmount())
				.discountAmount(dto.discount())
				.build();

		List<OrderItem> collect = dto.items().stream()
				.map(e -> buildOrderItem(e, order))
				.collect(Collectors.toList());
		order.setItems(collect);

		Order save = orderRepository.save(order);

		OrderCreatedEvent event = new OrderCreatedEvent(
				UUID.randomUUID(), save.getOrderId(), Instant.now(),
				dto.items(), save.getFinalAmount(), save.getUserId());
		
		OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(UUID.randomUUID(), save.getOrderId(), Instant.now(), dto.items(), save.getFinalAmount(), save.getUserId());

		KafkaTemplate.send(EventName.ORDER_CREATED_EVENT_V1,
				save.getOrderId().toString(), event);

		return save.getOrderId();
	}

	// ── Compensation: Inventory reservation failed ────────────────────────────

	/**
	 * Inventory could not be reserved — saga ends immediately, nothing to compensate.
	 * No stock was touched so no release is needed.
	 */
	@KafkaListener(
			topics  = EventName.INVENTORY_STOCK_RESERVED_FAILED_EVENT_V1,
			groupId = "order-service"
	)
	@Transactional
	public void onInventoryReservationFailed(InventoryReservationFailedEvent event) {
		log.warn("Order {} CANCELLED — inventory reservation failed: {}",
				event.orderId(), event.reason());
		orderRepository.updateStatus(event.orderId(), OrderStatus.CANCELLED);
	}

	// ── Compensation: Payment failed ──────────────────────────────────────────

	/**
	 * Payment failed — Payment Service has already published payment.failed.v1
	 * which will also trigger inventory release via inventory-service listener.
	 */
	@KafkaListener(
			topics  = EventName.PAYMENT_FAILED_EVENT_V1,
			groupId = "order-service"
	)
	@Transactional
	public void onPaymentFailed(PaymentFailedEvent event) {
		log.warn("Order {} CANCELLED — payment failed: {}",
				event.orderId(), event.reason());
		orderRepository.updateStatus(event.orderId(), OrderStatus.CANCELLED);
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	private OrderItem buildOrderItem(OrderItemDetailDTO e, Order order) {
		return OrderItem.builder()
				.productId(e.productId())
				.quantity(e.quantity())
				.unitPrice(e.unitPrice())
				.sku(e.sku())
				.torder(order)
				.build();
	}
}
