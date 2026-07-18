package com.ecommerce.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.model.Product;
import com.ecommerce.order.prodct.service.ProductService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(value = "/v1/products")
@AllArgsConstructor
public class ProductController {
	 private final ProductService productService;

	   
	    // @PostMapping handles HTTP POST requests to /v1/products
	    // @RequestBody reads the JSON from the request body and converts it to a Product object
	    // ResponseEntity lets us control the HTTP status code (201 Created, not just 200 OK)
	    @PostMapping
	    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
	        Product saved = productService.createProduct(product);
	        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	        // Returns: HTTP 201, body = {"productId": "...", "title": "...", ...}
	    }

	    // @GetMapping handles HTTP GET requests to /v1/products/{productId}
	    // @PathVariable extracts the {productId} from the URL
	    @GetMapping("/{productId}")
	    public ResponseEntity<Product> getProduct(@PathVariable UUID productId) {
	        Product product = productService.getProduct(productId);
	        return ResponseEntity.ok(product);
	        // Returns: HTTP 200, body = the product as JSON
	    }

	    // @RequestParam reads query parameters from the URL
	    // Example URL: /v1/products?category=Electronics
	    @GetMapping
	    public ResponseEntity<List<Product>> getProductsByCategory(
	            @RequestParam String category) {
	        List<Product> products = productService.getProductsByCategory(category);
	        return ResponseEntity.ok(products);
	    }
}
