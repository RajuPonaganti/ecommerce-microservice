package com.ecommerce.inventory.service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ecommerce.inventory.dtos.InventoryAvailabilityDTO;
import com.ecommerce.inventory.dtos.StockItemDTO;
import com.ecommerce.inventory.exception.StockNotFoundException;
import com.ecommerce.inventory.model.StockItem;
import com.ecommerce.inventory.repository.StockItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockItemSevice {

	private final StockItemRepository stockItemRepository;

	public ResponseEntity<StockItemDTO> addInventory(StockItemDTO dto) {
		log.info("StockItemSevice : addInventory (dto, productId) {} ", dto);

		StockItem stockItem = StockItem.builder()
				.productId(dto.getProductId())
				.waeHouseId(dto.getWaeHouseId())
				.quantityOnHand(dto.getQuantityOnHand())
				.quantityReserved(0)
				.quantityAvailable(dto.getQuantityOnHand())
				.build();

		StockItem saved = stockItemRepository.save(stockItem);

		StockItemDTO response = new StockItemDTO();
		response.setProductId(saved.getProductId());
		response.setWaeHouseId(saved.getWaeHouseId());
		response.setQuantityOnHand(saved.getQuantityOnHand());

		return ResponseEntity.ok(response);
	}

	public ResponseEntity<InventoryAvailabilityDTO> getAvailability(UUID productId, int requiredQuantity) {		log.info("StockItemSevice : getAvailability (productId, requiredQuantity) {} {}", productId, requiredQuantity);

		StockItem stockItem = stockItemRepository.findByProductId(productId)
				.orElseThrow(() -> new StockNotFoundException("Stock not found for productId: " + productId));

		int available = stockItem.getAvailableQuantity();

		return ResponseEntity.ok(new InventoryAvailabilityDTO(productId, available, requiredQuantity, available >= requiredQuantity));
	}
}
