package com.ecommerce.prodct.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.prodct.model.Product;
import com.ecommerce.prodct.model.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, UUID>{
	
	List<Product> findByStatusAndCategoryIgnoreCase(ProductStatus status, String category);

}
