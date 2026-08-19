package com.ecommerce.inventory.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.commons.EventName;
import com.ecommerce.commons.GroupIdName;
import com.ecommerce.commons.InventoryReleaseRequestedEvent;
import com.ecommerce.commons.InventoryReservationFailedEvent;
import com.ecommerce.commons.InventoryReservedEvent;
import com.ecommerce.commons.OrderCreatedEvent;
import com.ecommerce.commons.OrderItemDetailDTO;
import com.ecommerce.commons.PaymentCompletedEvent;
import com.ecommerce.commons.StockConfirmedEvent;
import com.ecommerce.inventory.dtos.InventoryAvailabilityDTO;
import com.ecommerce.inventory.dtos.InventoryDTO;
import com.ecommerce.inventory.exception.StockNotFoundException;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.StockReservation;
import com.ecommerce.inventory.model.StockReservationItem;
import com.ecommerce.inventory.model.StockReservationStatus;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.StockReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventorySevice {

	private final StockReservationRepository stockReservationRepository;
	private final InventoryRepository InventoryRepository;
	private final KafkaTemplate<String, Object> KafkaTemplate;

	// ── Add inventory ─────────────────────────────────────────────────────────

	public ResponseEntity<InventoryDTO> addInventory(InventoryDTO dto) {
		log.info("InventorySevice.addInventory() | productId={} | qty={}", dto.getProductId(), dto.getQuantityOnHand());

		Inventory inventory = Inventory.builder()
				.productId(dto.getProductId())
				.waeHouseId(dto.getWaeHouseId())
				.quantityOnHand(dto.getQuantityOnHand())
				.quantityReserved(0)
				.quantityAvailable(dto.getQuantityOnHand())
				.build();

		Inventory saved = InventoryRepository.save(inventory);
		log.info("InventorySevice.addInventory() | saved | stockItemId={}", saved.getStockItemId());

		InventoryDTO response = new InventoryDTO();
		response.setProductId(saved.getProductId());
		response.setWaeHouseId(saved.getWaeHouseId());
		response.setQuantityOnHand(saved.getQuantityOnHand());
		return ResponseEntity.ok(response);
	}

	// ── Check availability ────────────────────────────────────────────────────

	public ResponseEntity<InventoryAvailabilityDTO> getAvailability(UUID productId, int requiredQuantity) {
		log.debug("InventorySevice.getAvailability() | productId={} | required={}", productId, requiredQuantity);

		Inventory inventory = InventoryRepository.findByProductId(productId)
				.orElseThrow(() -> {
					log.error("InventorySevice.getAvailability() | not found | productId={}", productId);
					return new StockNotFoundException("Stock not found for productId: " + productId);
				});

		int available = inventory.getAvailableQuantity();
		boolean canOrder = available >= requiredQuantity;
		log.debug("InventorySevice.getAvailability() | available={} | required={} | canOrder={}", available, requiredQuantity, canOrder);

		return ResponseEntity.ok(new InventoryAvailabilityDTO(productId, available, requiredQuantity, canOrder));
	}

	// ── Kafka: OrderCreated → reserve stock ───────────────────────────────────

	@KafkaListener(topics = EventName.ORDER_CREATED_EVENT_V1, groupId = GroupIdName.INVENTORY_SERVICE)
	@Transactional
	public void onOrderCreatedEvent(OrderCreatedEvent event) {
		log.info("InventorySevice.onOrderCreatedEvent() | orderId={} | itemCount={}", event.orderId(), event.items().size());

		try {
			if (stockReservationRepository.existsByOrderId(event.orderId())) {
				log.warn("InventorySevice.onOrderCreatedEvent() | duplicate event — skipping | orderId={}", event.orderId());
				return;
			}

			List<OrderItemDetailDTO> items = event.items();
			for (OrderItemDetailDTO item : items) {
				log.debug("InventorySevice.onOrderCreatedEvent() | decrementing stock | productId={} | qty={}", item.productId(), item.quantity());
				int decrementStock = InventoryRepository.decrementStock(item.productId(), item.quantity());
				if (decrementStock == 0) {
					log.warn("InventorySevice.onOrderCreatedEvent() | insufficient stock | productId={}", item.productId());
					throw new RuntimeException("stock is not present for productId: " + item.productId());
				}
			}

			StockReservation sr = StockReservation.builder()
					.expiresAt(Instant.now().plus(2, ChronoUnit.DAYS))
					.orderId(event.orderId())
					.status(StockReservationStatus.RESERVED)
					.build();

			List<StockReservationItem> collect = items.stream()
					.map(e -> StockReservationItem.builder()
							.productId(e.productId()).quantity(e.quantity())
							.sellerId(e.sellerId()).sku(e.sku())
							.stockReservation(sr).build())
					.collect(Collectors.toList());
			sr.setStockReservationItemm(collect);
			StockReservation save = stockReservationRepository.save(sr);

			log.info("InventorySevice.onOrderCreatedEvent() | stock reserved | orderId={} | reservationId={}",
					event.orderId(), save.getReservationId());

			InventoryReservedEvent eventr = new InventoryReservedEvent(UUID.randomUUID(), event.orderId(), Instant.now(), save.getReservationId());
			KafkaTemplate.send(EventName.INVENTORY_STOCK_RESERVED_EVENT_V1,
					event.orderId().toString(), eventr);
		} catch (Exception e) {
			log.error("InventorySevice.onOrderCreatedEvent() | reservation failed | orderId={} | reason={}",
					event.orderId(), e.getMessage());
			InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
					UUID.randomUUID(), event.orderId(), Instant.now(), e.getMessage());
			KafkaTemplate.send(EventName.INVENTORY_STOCK_RESERVED_FAILED_EVENT_V1,
					event.orderId().toString(), failedEvent);
		}
	}

	// ── Kafka: PaymentFailed → release stock (compensation) ──────────────────

	@KafkaListener(topics = EventName.INVENTORY_RELEASE_REQUESTED_V1, groupId = GroupIdName.INVENTORY_SERVICE)
	@Transactional
	public void onInventoryReleaseRequested(InventoryReleaseRequestedEvent event) {
		log.info("InventorySevice.onInventoryReleaseRequested() | orderId={} | reason={}", event.orderId(), event.reason());

		try {
			StockReservation reservation = stockReservationRepository.findByOrderId(event.orderId()).orElse(null);

			if (reservation == null) {
				log.warn("InventorySevice.onInventoryReleaseRequested() | no reservation found | orderId={}", event.orderId());
				return;
			}

			if (reservation.getStatus() == StockReservationStatus.CANCELLED
					|| reservation.getStatus() == StockReservationStatus.EXPIRED) {
				log.warn("InventorySevice.onInventoryReleaseRequested() | already released | orderId={} | status={}", event.orderId(), reservation.getStatus());
				return;
			}

			List<StockReservationItem> items = reservation.getStockReservationItemm();
			for (StockReservationItem item : items) {
				int released = InventoryRepository.releaseStock(item.getProductId(), item.getQuantity());
				if (released == 0) {
					log.error("InventorySevice.onInventoryReleaseRequested() | release failed | productId={} qty={}", item.getProductId(), item.getQuantity());
				} else {
					log.info("InventorySevice.onInventoryReleaseRequested() | released {} units | productId={}", item.getQuantity(), item.getProductId());
				}
			}

			reservation.setStatus(StockReservationStatus.CANCELLED);
			stockReservationRepository.save(reservation);
			log.info("InventorySevice.onInventoryReleaseRequested() | stock released | orderId={}", event.orderId());

		} catch (Exception e) {
			log.error("InventorySevice.onInventoryReleaseRequested() | error during release | orderId={}", event.orderId(), e);
		}
	}

	// ── Kafka: PaymentCompleted → confirm reservation ─────────────────────────

	@KafkaListener(topics = EventName.PAYMENT_COMPLETED_EVENT_V1, groupId = GroupIdName.INVENTORY_SERVICE)
	@Transactional
	public void onPaymentCompleted(PaymentCompletedEvent event) {
		log.info("InventorySevice.onPaymentCompleted() | orderId={} | txnId={}", event.orderId(), event.paymentTransactionId());

		StockReservation reservation = stockReservationRepository.findByOrderId(event.orderId()).orElse(null);

		if (reservation == null) {
			log.warn("InventorySevice.onPaymentCompleted() | no reservation | orderId={}", event.orderId());
			return;
		}

		if (reservation.getStatus() == StockReservationStatus.CONFIRMED) {
			log.warn("InventorySevice.onPaymentCompleted() | already confirmed | orderId={}", event.orderId());
			return;
		}

		reservation.setStatus(StockReservationStatus.CONFIRMED);
		StockReservation saved = stockReservationRepository.save(reservation);

		List<StockReservationItem> items = saved.getStockReservationItemm();
		for (StockReservationItem item : items) {
			log.debug("InventorySevice.onPaymentCompleted() | confirming stock | productId={} qty={}", item.getProductId(), item.getQuantity());
			InventoryRepository.confirmStock(item.getProductId(), item.getQuantity());
		}

		StockConfirmedEvent confirmedEvent = new StockConfirmedEvent(
				event.orderId(), saved.getReservationId(), event.paymentTransactionId());
		KafkaTemplate.send(EventName.INVENTORY_STOCK_CONFIRMED_V1, event.orderId().toString(), confirmedEvent);

		log.info("InventorySevice.onPaymentCompleted() | stock confirmed | orderId={} | reservationId={}",
				event.orderId(), saved.getReservationId());
	}
}
