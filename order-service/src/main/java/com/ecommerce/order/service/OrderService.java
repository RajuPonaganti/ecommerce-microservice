package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.commons.EventName;
import com.ecommerce.commons.InventoryReservationFailedEvent;
import com.ecommerce.commons.OrderCreatedEvent;
import com.ecommerce.commons.OrderItemDetailDTO;
import com.ecommerce.commons.PaymentFailedEvent;
import com.ecommerce.order.clients.InventoryFeignClient;
import com.ecommerce.order.dtos.OrderCreateReqDTO;
import com.ecommerce.order.dtos.ProductResponse;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.exeception.ProductUnavailableException;
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
	private final InventoryFeignClient inventoryFeignClient;

	// ── Create Order ──────────────────────────────────────────────────────────

	@Transactional
	public UUID createOrder(OrderCreateReqDTO dto) {
		log.info("OrderService.createOrder() | userId={} | itemCount={} | totalAmount={}", dto.userId(),
				dto.items().size(), dto.totalAmount());// Step 1: resolve every line item against
														// product-catalog-service
		// (price + stock check) via Feign. A missing product or
		// insufficient stock fails the whole order up front, before we
		// ever try to take payment.
		BigDecimal total = BigDecimal.ZERO;

		List<OrderItemDetailDTO> items = dto.items();
		for (OrderItemDetailDTO orderItemDetailDTO : items) {
			ProductResponse product = inventoryFeignClient.getProduct(orderItemDetailDTO.productId());
			if (product == null) {
				throw new ProductUnavailableException("Product not found: " + orderItemDetailDTO.productId());
			}
			if (product.stockQuantity() < orderItemDetailDTO.quantity()) {
				throw new ProductUnavailableException(
						"Insufficient stock for product " + orderItemDetailDTO.productId() + " (requested "
								+ orderItemDetailDTO.quantity() + ", available " + product.stockQuantity() + ")");
			}
			total = total.add(product.price().multiply(BigDecimal.valueOf(orderItemDetailDTO.quantity())));
		}

		Order order = Order.builder().userId(dto.userId()).status(OrderStatus.CREATED).finalAmount(total)
				.discountAmount(dto.discount()).build();

		List<OrderItem> collect = dto.items().stream().map(e -> buildOrderItem(e, order)).collect(Collectors.toList());
		order.setItems(collect);

		Order save = orderRepository.save(order);

		OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), save.getOrderId(), Instant.now(),
				dto.items(), save.getFinalAmount(), save.getUserId());

		String traceId = MDC.get("traceId");

		ProducerRecord<String, Object> record = new ProducerRecord<>(EventName.ORDER_CREATED_EVENT_V1,
				save.getOrderId().toString(), event);

		if (traceId != null) {
			record.headers().add("traceId", traceId.getBytes(StandardCharsets.UTF_8));
		}

		KafkaTemplate.send(record);

		log.info("OrderService.createOrder() | order created | orderId={} | status=CREATED", save.getOrderId());
		return save.getOrderId();
	}

	// ── Compensation: Inventory reservation failed ────────────────────────────

	/**
	 * Inventory could not be reserved — saga ends immediately, nothing to
	 * compensate. No stock was touched so no release is needed.
	 */
	@KafkaListener(topics = EventName.INVENTORY_STOCK_RESERVED_FAILED_EVENT_V1, groupId = "order-service")
	@Transactional
	public void onInventoryReservationFailed(ConsumerRecord<String, InventoryReservationFailedEvent> record) {
		Header traceHeader = record.headers().lastHeader("traceId");
		String traceId = traceHeader != null ? new String(traceHeader.value(), StandardCharsets.UTF_8)
				: UUID.randomUUID().toString(); // fallback if missing

		MDC.put("traceId", traceId);
		try {
			InventoryReservationFailedEvent event = record.value();
			orderRepository.updateStatus(event.orderId(), OrderStatus.CANCELLED);
			log.warn("Order {} CANCELLED — inventory reservation failed: {}", event.orderId(), event.reason());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			MDC.remove("traceId");
		}

	}

	// ── Compensation: Payment failed ──────────────────────────────────────────

	/**
	 * Payment failed — Payment Service has already published payment.failed.v1
	 * which will also trigger inventory release via inventory-service listener.
	 */
	@KafkaListener(topics = EventName.PAYMENT_FAILED_EVENT_V1, groupId = "order-service")
	@Transactional
	public void onPaymentFailed(ConsumerRecord<String, PaymentFailedEvent> record) {
		Header traceHeader = record.headers().lastHeader("traceId");
		String traceId = traceHeader != null ? new String(traceHeader.value(), StandardCharsets.UTF_8)
				: UUID.randomUUID().toString(); // fallback if missing

		MDC.put("traceId", traceId);
		try {
			PaymentFailedEvent event = record.value();
			log.warn("Order {} CANCELLED — payment failed: {}", event.orderId(), event.reason());
			orderRepository.updateStatus(event.orderId(), OrderStatus.CANCELLED);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			MDC.remove("traceId");
		}
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	private OrderItem buildOrderItem(OrderItemDetailDTO e, Order order) {
		return OrderItem.builder().productId(e.productId()).quantity(e.quantity()).unitPrice(e.unitPrice()).sku(e.sku())
				.torder(order).build();
	}
}
