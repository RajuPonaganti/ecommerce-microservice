package com.ecommerce.product.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.product.dto.ProductCreateDto;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductUpdateDto;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

	private final ProductRepository productRepository;

	@Transactional
	public ProductDto createProduct(ProductCreateDto dto, UUID sellerId) {
		log.info("ProductService.createProduct() | sellerId={} | sku={}", sellerId, dto.getSku());

		Product product = new Product();
		product.setTitle(dto.getTitle());
		product.setDescription(dto.getDescription());
		product.setBrand(dto.getBrand());
		product.setPrice(dto.getPrice());
		product.setMrp(dto.getMrp());
		product.setCurrency(dto.getCurrency());
		product.setCategory(dto.getCategory());
		product.setSku(dto.getSku());
		product.setAttributes(dto.getAttributes());
		product.setSellerId(sellerId);
		product.setStatus(ProductStatus.DRAFT);
		product.setCreatedAt(Instant.now());

		ProductDto result = toDto(productRepository.save(product));
		log.info("ProductService.createProduct() | created | productId={}", result.getProductId());
		return result;
	}

	@Transactional(readOnly = true)
	public ProductDto getProduct(UUID productId) {
		log.debug("ProductService.getProduct() | productId={}", productId);
		return productRepository.findById(productId).map(p -> {
			log.debug("ProductService.getProduct() | found | title={}", p.getTitle());
			return toDto(p);
		}).orElseThrow(() -> {
			log.error("ProductService.getProduct() | not found | productId={}", productId);
			return new ProductNotFoundException(productId);
		});
	}

	@Transactional
	public ProductDto updateProduct(UUID productId, ProductUpdateDto dto, UUID sellerId) {
		log.info("ProductService.updateProduct() | productId={} | sellerId={}", productId, sellerId);

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> {
					log.error("ProductService.updateProduct() | not found | productId={}", productId);
					return new ProductNotFoundException(productId);
				});

		if (!product.getSellerId().equals(sellerId)) {
			log.warn("ProductService.updateProduct() | access denied | productId={} | sellerId={}", productId, sellerId);
			throw new ProductAccessDeniedException(productId, sellerId);
		}

		if (dto.getTitle() != null)       product.setTitle(dto.getTitle());
		if (dto.getDescription() != null) product.setDescription(dto.getDescription());
		if (dto.getBrand() != null)       product.setBrand(dto.getBrand());
		if (dto.getPrice() != null)       product.setPrice(dto.getPrice());
		if (dto.getMrp() != null)         product.setMrp(dto.getMrp());
		if (dto.getCurrency() != null)    product.setCurrency(dto.getCurrency());
		if (dto.getCategory() != null)    product.setCategory(dto.getCategory());
		if (dto.getAttributes() != null)  product.setAttributes(dto.getAttributes());
		product.setUpdatedAt(Instant.now());

		ProductDto result = toDto(productRepository.save(product));
		log.info("ProductService.updateProduct() | updated | productId={}", productId);
		return result;
	}

	@Transactional(readOnly = true)
	public Page<ProductDto> listProducts(String category, String brand, Pageable pageable) {
		log.debug("ProductService.listProducts() | category={} | brand={} | page={}", category, brand, pageable.getPageNumber());

		Page<Product> page;
		if (category != null && brand != null) {
			page = productRepository.findByCategoryAndBrand(category, brand, pageable);
		} else if (category != null) {
			page = productRepository.findByCategory(category, pageable);
		} else if (brand != null) {
			page = productRepository.findByBrand(brand, pageable);
		} else {
			page = productRepository.findAll(pageable);
		}

		log.debug("ProductService.listProducts() | returned {} products", page.getTotalElements());
		return page.map(this::toDto);
	}

	@Transactional
	public ProductDto publishProduct(UUID productId, UUID sellerId) {
		log.info("ProductService.publishProduct() | productId={} | sellerId={}", productId, sellerId);

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> {
					log.error("ProductService.publishProduct() | not found | productId={}", productId);
					return new ProductNotFoundException(productId);
				});

		if (!product.getSellerId().equals(sellerId)) {
			log.warn("ProductService.publishProduct() | access denied | productId={} | sellerId={}", productId, sellerId);
			throw new ProductAccessDeniedException(productId, sellerId);
		}

		product.setStatus(ProductStatus.ACTIVE);
		product.setUpdatedAt(Instant.now());
		ProductDto result = toDto(productRepository.save(product));
		log.info("ProductService.publishProduct() | published | productId={}", productId);
		return result;
	}

	private ProductDto toDto(Product p) {
		ProductDto dto = new ProductDto();
		dto.setProductId(p.getProductId());
		dto.setTitle(p.getTitle());
		dto.setDescription(p.getDescription());
		dto.setBrand(p.getBrand());
		dto.setPrice(p.getPrice());
		dto.setMrp(p.getMrp());
		dto.setCurrency(p.getCurrency());
		dto.setCategory(p.getCategory());
		dto.setStatus(p.getStatus());
		dto.setAttributes(p.getAttributes());
		dto.setSellerId(p.getSellerId());
		dto.setSku(p.getSku());
		dto.setCreatedAt(p.getCreatedAt());
		dto.setUpdatedAt(p.getUpdatedAt());
		dto.setVersion(p.getVersion());
		return dto;
	}

	public static class ProductNotFoundException extends RuntimeException {
		public ProductNotFoundException(UUID productId) {
			super("Product not found: " + productId);
		}
	}

	public static class ProductAccessDeniedException extends RuntimeException {
		public ProductAccessDeniedException(UUID productId, UUID sellerId) {
			super("Seller " + sellerId + " does not own product " + productId);
		}
	}

	public ProductResponse getProductForOrder(UUID productId) {
		 Optional<Product> byId = productRepository.findById(productId);
		 if(byId.isPresent()) {
			 Product product = byId.get();
			 return new ProductResponse(product.getProductId(), product.getTitle(), product.getPrice(), null);
		 }
			 
		return null;
	}
}
