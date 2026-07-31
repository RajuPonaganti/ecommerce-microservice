package com.ecommerce.order.clients;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.order.dtos.InventoryAvailabilityDTO;

@FeignClient(name="inventory-service")
public interface NotificationFeignClient {
	@GetMapping(value = "/v1/inventory/availability/{productId}")
	public InventoryAvailabilityDTO getAvailability(
			@PathVariable UUID productId,
			@RequestParam int requiredQuantity);

	public void sendSuccess(UUID orderId);

	public void sendFailure(UUID orderId);

}
