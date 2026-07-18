package com.ecommerce.prodct.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.prodct.model.Product;
import com.ecommerce.prodct.model.ProductStatus;
import com.ecommerce.prodct.repository.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
	
	private final ProductRepository productRepository;
	
	public Product createProduct(Product product) {
        product.setStatus(ProductStatus.DRAFT); // always start as draft
        return productRepository.save(product);
        // .save() runs: INSERT INTO products (product_id, title, price, ...) VALUES (...)
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByStatusAndCategoryIgnoreCase(ProductStatus.ACTIVE, category);
    }

    public Product getProduct(UUID productId) {
        // findById returns Optional<Product> — it might be empty if ID does not exist
        return productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        // orElseThrow — if Optional is empty, throw an exception
    }

}
