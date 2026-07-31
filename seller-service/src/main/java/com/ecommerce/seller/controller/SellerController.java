package com.ecommerce.seller.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.seller.dto.SellerDTO;
import com.ecommerce.seller.service.SellerService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/v1/seller")
@AllArgsConstructor
public class SellerController {

	private final SellerService sellerService;
	
	@PostMapping
	public ResponseEntity<SellerDTO> saveSeller( @RequestBody final SellerDTO dto){
		
		return ResponseEntity.status(HttpStatus.CREATED).body(sellerService.saveSeller(dto)) ;
	}
}
