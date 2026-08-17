package com.ecommerce.order.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.dtos.OrderCreateReqDTO;
import com.ecommerce.order.dtos.OrderCreateRespDTO;
import com.ecommerce.order.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/v1/orders")
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<UUID> createOrder(@RequestBody OrderCreateReqDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(dto));

	}
}
