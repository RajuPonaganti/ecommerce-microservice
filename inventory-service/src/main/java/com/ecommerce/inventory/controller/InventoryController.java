package com.ecommerce.inventory.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.inventory.dtos.InventoryAvailabilityDTO;
import com.ecommerce.inventory.dtos.StockItemDTO;
import com.ecommerce.inventory.service.StockItemSevice;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/v1/inventory")
@AllArgsConstructor
public class InventoryController {

	private final StockItemSevice service;

	@PostMapping
	public ResponseEntity<?> addInventory(@Valid @RequestBody StockItemDTO dto, @RequestHeader UUID productId) {
		return service.addInventory(dto, productId);
	}

	// GET /v1/inventory/{productId}/availability?requiredQuantity=
	// availableForOrdering = true only if available stock >= requiredQuantity
	@GetMapping("/availability/{productId}")
	public ResponseEntity<InventoryAvailabilityDTO> getAvailability(
			@PathVariable UUID productId,
			@RequestParam(defaultValue = "1") int requiredQuantity) {
		return service.getAvailability(productId, requiredQuantity);
	}
}
