package com.ecommerce.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.dtos.OrderCreateReqDTO;
import com.ecommerce.order.dtos.OrderCreateRespDTO;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.service.OrderService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping(value = "/v1/orders")
@AllArgsConstructor
public class OrderController {
private final OrderService orderService;
public ResponseEntity<OrderCreateRespDTO>  createOrder(@RequestBody OrderCreateReqDTO dto){
	OrderCreateRespDTO resp = orderService.createOrder(dto);
	return ResponseEntity.status(HttpStatus.CREATED).body(null);
	
}
}
