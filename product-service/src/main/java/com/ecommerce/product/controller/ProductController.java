package com.ecommerce.product.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.product.dto.ProductCreateDto;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.dto.ProductUpdateDto;
import com.ecommerce.product.service.ProductService;
import com.ecommerce.product.service.ProductService.ProductAccessDeniedException;
import com.ecommerce.product.service.ProductService.ProductNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	// POST /v1/products — JWT (SELLER), sellerId injected by gateway as X-Seller-Id
	@PostMapping
	public ResponseEntity<ProductDto> createProduct(
			@Valid @RequestBody final ProductCreateDto dto,
			@RequestHeader("X-Seller-Id") UUID sellerId) {

		ProductDto created = productService.createProduct(dto, sellerId);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// GET /v1/products/{productId} — public
	@GetMapping("/{productId}")
	public ResponseEntity<ProductDto> getProduct(@PathVariable final UUID productId) {
		return ResponseEntity.ok(productService.getProduct(productId));
	}

	// PUT /v1/products/{productId} — JWT (SELLER, owns product)
	@PutMapping("/{productId}")
	public ResponseEntity<ProductDto> updateProduct(
			@PathVariable UUID productId,
			@Valid @RequestBody ProductUpdateDto dto,
			@RequestHeader("X-Seller-Id") UUID sellerId) {

		return ResponseEntity.ok(productService.updateProduct(productId, dto, sellerId));
	}

	// GET /v1/products?category=&brand=&page= — public
	@GetMapping
	public ResponseEntity<Page<ProductDto>> listProducts(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String brand,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Page<ProductDto> result = productService.listProducts(category, brand, PageRequest.of(page, size));
		return ResponseEntity.ok(result);
	}

	// POST /v1/products/{productId}/publish — JWT (SELLER)
	@PostMapping("/{productId}/publish")
	public ResponseEntity<ProductDto> publishProduct(
			@PathVariable final UUID productId,
			@RequestHeader("X-Seller-Id") final UUID sellerId) {

		return ResponseEntity.ok(productService.publishProduct(productId, sellerId));
	}

	// --- Exception handlers (RFC 7807 Problem Details) ---

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(ProductNotFoundException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		pd.setTitle("Product Not Found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
	}

	@ExceptionHandler(ProductAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(ProductAccessDeniedException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		pd.setTitle("Access Denied");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
	}
}
