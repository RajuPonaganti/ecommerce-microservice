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

import com.ecommerce.inventory.dtos.InventoryAvailabilityDTO;
import com.ecommerce.inventory.dtos.InventoryDTO;
import com.ecommerce.inventory.events.EventName;
import com.ecommerce.inventory.events.GroupIdName;
import com.ecommerce.inventory.events.OrderCreatedEvent;
import com.ecommerce.inventory.events.OrderItemDetailDTO;
import com.ecommerce.inventory.events.InventoryReservationFailedEvent;
import com.ecommerce.inventory.events.InventoryReservedEvent;
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

	public ResponseEntity<InventoryDTO> addInventory(InventoryDTO dto) {
		log.info("InventorySevice : addInventory (dto, productId) {} ", dto);

		Inventory inventory = Inventory.builder().productId(dto.getProductId()).waeHouseId(dto.getWaeHouseId())
				.quantityOnHand(dto.getQuantityOnHand()).quantityReserved(0).quantityAvailable(dto.getQuantityOnHand())
				.build();

		Inventory saved = InventoryRepository.save(inventory);

		InventoryDTO response = new InventoryDTO();
		response.setProductId(saved.getProductId());
		response.setWaeHouseId(saved.getWaeHouseId());
		response.setQuantityOnHand(saved.getQuantityOnHand());

		return ResponseEntity.ok(response);
	}

	public ResponseEntity<InventoryAvailabilityDTO> getAvailability(UUID productId, int requiredQuantity) {
		log.info("InventorySevice : getAvailability (productId, requiredQuantity) {} {}", productId, requiredQuantity);

		Inventory Inventory = InventoryRepository.findByProductId(productId)
				.orElseThrow(() -> new StockNotFoundException("Stock not found for productId: " + productId));

		int available = Inventory.getAvailableQuantity();

		return ResponseEntity.ok(
				new InventoryAvailabilityDTO(productId, available, requiredQuantity, available >= requiredQuantity));
	}

	@KafkaListener(topics = EventName.ORDER_CREATED_EVENT_V1, groupId = GroupIdName.INVENTORY_SERVICE)
	@Transactional
	public void onOrderCreatedEvent(OrderCreatedEvent event) {
		try {
			if (stockReservationRepository.existsByOrderId(event.orderId()))
				return;
			List<OrderItemDetailDTO> items = event.items();

			for (OrderItemDetailDTO item : items) {
				int decrementStock = InventoryRepository.decrementStock(item.productId(), item.quantity());
				if (decrementStock == 0)
					throw new RuntimeException("stock is not present");
			}

			// Let @GeneratedValue assign the PK — never set @Id manually on a
			// @GeneratedValue entity
			StockReservation sr = StockReservation.builder().expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
					.orderId(event.orderId()).status(StockReservationStatus.RESERVED).build();
			List<StockReservationItem> collect = items.stream()
					.map(e -> StockReservationItem.builder().productId(e.productId()).quantity(e.quantity())
							.sellerId(e.sellerId()).sku(e.sku()).stockReservation(sr).build())
					.collect(Collectors.toList());
			sr.setStockReservationItemm(collect);
			StockReservation save = stockReservationRepository.save(sr);
			InventoryReservedEvent inventoryReservedEvent = new InventoryReservedEvent(UUID.randomUUID(), event.orderId(),
					Instant.now(), save.getReservationId());
			KafkaTemplate.send(EventName.INVENTORY_STOCK_RESERVED_EVENT_V1, save.getReservationId().toString(),
					inventoryReservedEvent);

		} catch (Exception e) {
			InventoryReservationFailedEvent reservationFailedEvent = new InventoryReservationFailedEvent(UUID.randomUUID(),
					event.orderId(), Instant.now(), e.getMessage());
			KafkaTemplate.send(EventName.INVENTORY_STOCK_RESERVED_FAILED_EVENT_V1, event.orderId().toString(),
					reservationFailedEvent);

		}
	}
}
